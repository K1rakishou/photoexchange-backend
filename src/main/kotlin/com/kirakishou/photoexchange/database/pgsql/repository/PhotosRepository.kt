package com.kirakishou.photoexchange.database.pgsql.repository

import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.mongo.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.mongo.dao.LocationMapDao
import com.kirakishou.photoexchange.database.mongo.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.mongo.dao.ReportedPhotoDao
import com.kirakishou.photoexchange.database.mongo.entity.GalleryPhoto
import com.kirakishou.photoexchange.database.mongo.repository.AbstractRepository
import com.kirakishou.photoexchange.database.pgsql.dao.FavouritedPhotosDao
import com.kirakishou.photoexchange.database.pgsql.dao.PhotosDao
import com.kirakishou.photoexchange.database.pgsql.dao.UsersDao
import com.kirakishou.photoexchange.database.pgsql.entity.PhotoEntity
import com.kirakishou.photoexchange.exception.DatabaseTransactionException
import com.kirakishou.photoexchange.exception.ExchangeException
import com.kirakishou.photoexchange.service.DiskManipulationService
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactive.awaitFirst
import net.response.data.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.IOException

open class PhotosRepository(
  private val mongoSequenceDao: MongoSequenceDao,
  private val photosDao: PhotosDao,
  private val usersDao: UsersDao,
  private val galleryPhotoDao: GalleryPhotoDao,
  private val favouritedPhotosDao: FavouritedPhotosDao,
  private val reportedPhotoDao: ReportedPhotoDao,
  private val locationMapDao: LocationMapDao,
  private val generator: GeneratorService,
  private val diskManipulationService: DiskManipulationService,
  dispatcher: CoroutineDispatcher
) : AbstractRepository(dispatcher) {
  private val logger = LoggerFactory.getLogger(PhotosRepository::class.java)

  private fun generatePhotoInfoName(): PhotoName {
    var photoName = PhotoName.empty()

    while (true) {
      val generatedName = generator.generateNewPhotoName()

      val found = transaction {
        photosDao.photoNameExists(generatedName)
      }

      if (!found) {
        photoName = generatedName
        break
      }
    }

    return photoName
  }

  suspend fun save(userId: UserId, lon: Double, lat: Double, isPublic: Boolean, uploadedOn: Long, ipHash: IpHash): Photo {
    return dbQuery(Photo.empty()) {
      val photoName = generatePhotoInfoName()

      val photoInfo = PhotoEntity.create(
        userId,
        photoName,
        isPublic,
        lon,
        lat,
        uploadedOn,
        ipHash
      )

      val savedPhotoInfo = photosDao.save(photoInfo)
      if (savedPhotoInfo.isEmpty()) {
        return@dbQuery Photo.empty()
      }

      if (photoInfo.isPublic) {
        //TODO
        val galleryPhotoId = mongoSequenceDao.getNextGalleryPhotoId().awaitFirst()
        val newGalleryPhoto = GalleryPhoto.create(
          galleryPhotoId,
          photoInfo.photoName.name,
          savedPhotoInfo.uploadedOn
        )

        val result = galleryPhotoDao.save(newGalleryPhoto).awaitFirst()
        if (!result) {
          photosDao.deleteById(photoInfo.photoId)
          return@dbQuery Photo.empty()
        }
      }

      return@dbQuery savedPhotoInfo.toPhoto()
    }
  }

  open suspend fun findOneById(photoId: PhotoId): Photo {
    return dbQuery(Photo.empty()) {
      return@dbQuery photosDao.findById(photoId).toPhoto()
    }
  }

  suspend fun findOneByPhotoName(photoName: PhotoName): Photo {
    return dbQuery(Photo.empty()) {
      return@dbQuery photosDao.findByPhotoName(photoName).toPhoto()
    }
  }

  suspend fun findAllPhotosByUserId(userUuid: UserUuid): List<Photo> {
    return dbQuery(emptyList()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery emptyList<Photo>()
      }

      return@dbQuery photosDao.findAllByUserId(user.id)
        .map { photoEntity -> photoEntity.toPhoto() }
    }
  }

  suspend fun findPageOfUploadedPhotos(
    userUuid: UserUuid,
    lastUploadedOn: Long,
    count: Int
  ): List<UploadedPhotoResponseData> {
    return dbQuery(emptyList()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery emptyList<UploadedPhotoResponseData>()
      }

      val myPhotos = photosDao.findPageOfUploadedPhotos(user.id, lastUploadedOn, count)

      val exchangeIds = myPhotos.map { it.exchangedPhotoId }
      val theirPhotos = photosDao.findManyByExchangedIdList(exchangeIds)

      val result = mutableListOf<UploadedPhotoResponseData>()

      for (myPhoto in myPhotos) {
        val theirPhoto = theirPhotos.firstOrNull { it.photoId.id == myPhoto.exchangedPhotoId.id }
        val receiverInfo = theirPhoto
          ?.let { ReceiverInfoResponseData(it.photoName.name, it.lon, it.lat) }

        result += UploadedPhotoResponseData(
          myPhoto.photoId.id,
          myPhoto.photoName.name,
          myPhoto.lon,
          myPhoto.lat,
          receiverInfo,
          myPhoto.uploadedOn
        )
      }

      return@dbQuery result
    }
  }

  suspend fun findPageOfReceivedPhotos(
    userUuid: UserUuid,
    lastUploadedOn: Long,
    count: Int
  ): List<ReceivedPhotoResponseData> {
    return dbQuery(emptyList()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery emptyList<ReceivedPhotoResponseData>()
      }

      val myPhotos = photosDao.findPageOfReceivedPhotos(user.id, lastUploadedOn, count)
      val theirPhotoIds = myPhotos.map { it.exchangedPhotoId }

      val theirPhotos = photosDao.findManyByExchangedIdList(theirPhotoIds, false)
      val result = mutableListOf<ReceivedPhotoResponseData>()

      for (theirPhoto in theirPhotos) {
        val myPhoto = myPhotos.firstOrNull { it.photoId.id == theirPhoto.exchangedPhotoId.id }
        if (myPhoto == null) {
          logger.warn(
            "Could not find myPhoto by theirPhoto.exchangedPhotoId (${theirPhoto.exchangedPhotoId}). " +
              "This may be because it got deleted without theirPhoto being deleted as well (inconsistency)."
          )
          continue
        }

        result += ReceivedPhotoResponseData(
          theirPhoto.photoId.id,
          myPhoto.photoName.name,
          theirPhoto.photoName.name,
          theirPhoto.lon,
          theirPhoto.lat,
          theirPhoto.uploadedOn
        )
      }

      return@dbQuery result
    }
  }

  /**
   * Finds the "count" amount of photos that were uploaded earlier than "uploadedEarlierThanTime". Then groups them in pairs
   * "photo - exchanged photo" and checks that both of the photos we uploaded earlier than "uploadedEarlierThanTime".
   * If it's true then adds them to the list of photos which deletedOn is to be updated with "currentTime" and after that
   * makes an update.
   *
   * @param uploadedEarlierThanTime - photos should be uploaded earlier than this date
   * @param currentTime - current time
   * @param count - the maximum amount of photos to be found
   * @return -1 if something went wrong or amount of updated photos
   * */
  suspend fun markAsDeletedPhotosUploadedEarlierThan(
    uploadedEarlierThanTime: Long,
    currentTime: Long,
    count: Int
  ): Int {
    return dbQuery(-1) {
      try {
        val oldPhotos = photosDao.findOldPhotos(uploadedEarlierThanTime, count)
        val oldPhotosMap = oldPhotos.associateBy { it.photoId.id }
        val exchangedPhotosMap = oldPhotos.associateBy { it.exchangedPhotoId.id }

        val toBeUpdated = hashSetOf<Long>()

        for (photo in oldPhotosMap) {
          if (toBeUpdated.contains(photo.key)) {
            continue
          }

          val exchangedPhoto = exchangedPhotosMap[photo.key]
          if (exchangedPhoto == null) {
            continue
          }

          if (toBeUpdated.contains(exchangedPhoto.photoId.id)) {
            continue
          }

          if (photo.value.photoId.id != exchangedPhoto.exchangedPhotoId.id
            || photo.value.exchangedPhotoId.id != exchangedPhoto.photoId.id) {

            // Photos do not have each other's ids for some unknown reason. We can't delete such photos because
            // this may create inconsistency.
            logger.debug(
              "exchanged photos does not have each other's ids: " +
                "photo.value.photoId = ${photo.value.photoId}, " +
                "exchangedPhoto.exchangedPhotoId = ${exchangedPhoto.exchangedPhotoId}, " +
                "photo.value.exchangedPhotoId = ${photo.value.exchangedPhotoId}, " +
                "exchangedPhoto.photoId = ${exchangedPhoto.photoId}"
            )
            continue
          }

          //if both of the photos were uploaded earlier than "uploadedEarlierThanTime" - add them both to the list
          if (photo.value.uploadedOn < uploadedEarlierThanTime
            && exchangedPhoto.uploadedOn < uploadedEarlierThanTime) {

            toBeUpdated.add(photo.key)
            toBeUpdated.add(exchangedPhoto.photoId.id)
          }
        }

        //update photos as deleted
        val photoIdList = toBeUpdated.map { PhotoId(it) }.toList()
        if (!photosDao.updateManyAsDeleted(currentTime, photoIdList)) {
          return@dbQuery -1
        }

        return@dbQuery toBeUpdated.size
      } catch (error: Throwable) {
        logger.error("Could not mark photos as deleted", error)
        return@dbQuery -1
      }
    }
  }

  /**
   * Find "count" amount of photos with "deletedOn" field greater than zero and deletes them one by one.
   * After that deletes all files associated with those photos.
   *
   * @param deletedEarlierThanTime - photos should be marked as deleted earlier than this date
   * @param count - the maximum amount of photos to be found
   * */
  suspend fun cleanDatabaseAndPhotos(
    deletedEarlierThanTime: Long,
    count: Int
  ) {
    dbQuery(Unit) {
      val photosToDelete = photosDao.findDeletedPhotos(deletedEarlierThanTime, count)
      if (photosToDelete.isEmpty()) {
        logger.debug("No photos to delete")
        return@dbQuery
      }

      logger.debug("Found ${photosToDelete.size} photos to delete")
      val photoFilesToDelete = mutableListOf<String>()

      for (photoEntity in photosToDelete) {
        logger.debug("Deleting ${photoEntity.photoName}")

        //TODO: probably should rewrite this to delete all photos in one transaction
        if (!delete(photoEntity.photoId, photoEntity.photoName)) {
          logger.error("Could not deletePhotoWithFile photo ${photoEntity.photoName}")
          continue
        }

        photoFilesToDelete += photoEntity.photoName.name
      }

      if (photoFilesToDelete.isNotEmpty()) {
        photoFilesToDelete.forEach {
          try {
            diskManipulationService.deleteAllPhotoFiles(it)
          } catch (error: IOException) {
            // If an error occurs here just skip the file. There would be files left on the disk but at least
            // that won't stop the whole process
            logger.error("Error while trying to delete files of photo with name (${it})")
          }
        }
      }
    }
  }

  /**
   * Tries to find the oldest not exchanged photo and exchange it with the current photo.
   * If it cannot find oldest not exchanged photo then sets exchangedPhotoId of the newUploadingPhoto to EMPTY_PHOTO_ID
   * and returns empty PhotoInfo.
   * If it can then it sets exchangedPhotoId of both photos to each others in transaction.
   * If transaction fails it sets exchangedPhotoId of the newUploadingPhoto to EMPTY_PHOTO_ID and returns empty PhotoInfo.
   * If it doesn't fail then it returns the oldestPhoto with updated exchangedPhotoId with newUploadingPhoto.photoId.
   * */
  open suspend fun tryDoExchange(userUuid: UserUuid, newUploadingPhoto: Photo): Photo {
    return dbQuery(Photo.empty()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery Photo.empty()
      }

      val oldestPhoto = photosDao.findOldestEmptyPhoto(user.id)
      if (oldestPhoto.isEmpty()) {
        if (!photosDao.updatePhotoAsEmpty(newUploadingPhoto.photoId)) {
          throw ExchangeException("Could not set photoExchangeId as EMPTY_PHOTO_ID")
        }

        return@dbQuery Photo.empty()
      }

      val transactionResult = try {
        transaction {
          photosDao.updatePhotoSetReceiverId(
            oldestPhoto.photoId,
            ExchangedPhotoId(newUploadingPhoto.photoId.id)
          )

          photosDao.updatePhotoSetReceiverId(
            newUploadingPhoto.photoId,
            ExchangedPhotoId(oldestPhoto.photoId.id)
          )

          return@transaction true
        }
      } catch (error: DatabaseTransactionException) {
        false
      }

      if (!transactionResult) {
        if (!photosDao.updatePhotoAsEmpty(newUploadingPhoto.photoId)) {
          throw ExchangeException("Could not set photoExchangeId as EMPTY_PHOTO_ID")
        }

        return@dbQuery Photo.empty()
      }

      return@dbQuery oldestPhoto.toPhoto()
        .copy(exchangedPhotoId = ExchangedPhotoId(newUploadingPhoto.photoId.id))
    }
  }

  //FIXME: doesn't work in tests
  //should be called from within locked mutex
  open suspend fun delete(photoId: PhotoId, photoName: PhotoName): Boolean {
    return dbQuery(false) {
      return@dbQuery try {
        transaction {
          //TODO:
//          photosDao.deleteById(photoId)
//          favouritedPhotoDao.deleteFavouriteByPhotoName(photoName)
//          reportedPhotoDao.deleteReportByPhotoName(photoName)
//          locationMapDao.deleteById(photoId)
//          galleryPhotoDao.deleteByPhotoName(photoName)
        }

        true
      } catch (error: Throwable) {
        logger.error("Could not delete photo", error)
        false
      }
    }
  }

  //TODO
//  suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoResult {
//    return dbQuery {
//      return@dbQuery if (favouritedPhotoDao.isPhotoFavourited(userId, photoName)) {
//        if (!favouritedPhotoDao.unfavouritePhoto(userId, photoName)) {
//          return@dbQuery FavouritePhotoResult.Error
//        }
//
//        val favouritesCount = favouritedPhotoDao.countFavouritesByPhotoName(photoName)
//        FavouritePhotoResult.Unfavourited(favouritesCount)
//      } else {
//        val id = mongoSequenceDao.getNextFavouritedPhotoId()
//        if (!favouritedPhotoDao.favouritePhoto(FavouritedPhoto.create(id, photoName, userId))) {
//          return@dbQuery FavouritePhotoResult.Error
//        }
//
//        val favouritesCount = favouritedPhotoDao.countFavouritesByPhotoName(photoName)
//        FavouritePhotoResult.Favourited(favouritesCount)
//      }
//    }
//  }

  //TODO
//  suspend fun reportPhoto(userId: String, photoName: String): ReportPhotoResult {
//
//    return dbQuery {
//      return@dbQuery if (reportedPhotoDao.isPhotoReported(userId, photoName)) {
//        if (!reportedPhotoDao.unreportPhoto(userId, photoName)) {
//          return@dbQuery ReportPhotoResult.Error
//        }
//
//        ReportPhotoResult.Unreported
//      } else {
//        val id = mongoSequenceDao.getNextReportedPhotoId()
//        if (!reportedPhotoDao.reportPhoto(ReportedPhoto.create(id, photoName, userId))) {
//          return@dbQuery ReportPhotoResult.Error
//        }
//
//        ReportPhotoResult.Reported
//      }
//    }
//  }

  suspend fun findGalleryPhotos(lastUploadedOn: Long, count: Int): List<GalleryPhotoResponseData> {
    return dbQuery(emptyList()) {
      val pageOfGalleryPhotos = galleryPhotoDao.findPage(lastUploadedOn, count).awaitFirst()

      val resultMap = linkedMapOf<Long, GalleryPhotoDto>()
      val photoNameList = pageOfGalleryPhotos.map { PhotoName(it.photoName) }

      val photoInfos = photosDao.findPhotosByName(photoNameList)
      for (photo in photoInfos) {
        val galleryPhoto = pageOfGalleryPhotos.first { it.photoName == photo.photoName.name }
        resultMap[photo.photoId.id] = GalleryPhotoDto(
          photo.toPhoto(),
          galleryPhoto
        )
      }

      return@dbQuery resultMap.values.map { (photoInfo, _) ->
        GalleryPhotoResponseData(
          photoInfo.photoName.name,
          photoInfo.lon,
          photoInfo.lat,
          photoInfo.uploadedOn
        )
      }
    }
  }

  suspend fun findPhotoAdditionalInfo(
    userUuid: UserUuid,
    photoNames: List<PhotoName>
  ): List<PhotoAdditionalInfoResponseData> {
    return dbQuery(emptyList()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery emptyList<PhotoAdditionalInfoResponseData>()
      }

      val photoList = photosDao.findManyByPhotoNameList(photoNames)

      val photoMap = photoList.associateBy { it.photoId }
      val photoIdList = photoList.map { it.photoId }

      val resultMap = linkedMapOf<String, GalleryPhotoInfoDto>()
      val countMap = favouritedPhotosDao.countFavouritesByPhotoIdList(photoIdList)

      val userFavouritedPhotos = favouritedPhotosDao.findManyFavouritedPhotos(user.id, photoIdList)
      //TODO
      val userReportedPhotos = reportedPhotoDao.findManyReportsByPhotoNameList(user.id, photoIdList)

      for (favouritedPhoto in userFavouritedPhotos) {
        val photoName = photoMap[favouritedPhoto.photoId]!!.photoName.name

        resultMap.putIfAbsent(photoName, GalleryPhotoInfoDto(favouritedPhoto.photoId.id, photoName))
        resultMap[photoName]!!.isFavourited = true
      }

      //TODO
      for (reportedPhoto in userReportedPhotos) {
        //TODO
        resultMap.putIfAbsent(reportedPhoto.photoName, GalleryPhotoInfoDto(reportedPhoto.photoName))
        resultMap[reportedPhoto.photoName]!!.isReported = true
      }

      return@dbQuery resultMap.values.map { (galleryPhotoId, galleryPhotoName, isFavourited, isReported) ->
        PhotoAdditionalInfoResponseData(
          galleryPhotoName,
          isFavourited,
          countMap.getOrDefault(galleryPhotoId, 0L),
          isReported
        )
      }
    }
  }

  suspend fun findPhotosWithReceiverByPhotoNamesList(
    userUuid: UserUuid,
    photoNameList: List<PhotoName>
  ): List<ReceivedPhotoResponseData> {
    return dbQuery(emptyList()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery emptyList<ReceivedPhotoResponseData>()
      }

      val myPhotos = photosDao.findPhotosByNames(user.id, photoNameList)
      val theirPhotoIds = myPhotos.map { it.exchangedPhotoId }

      val theirPhotos = photosDao.findManyByExchangedIdList(theirPhotoIds)
      val result = mutableListOf<ReceivedPhotoResponseData>()

      for (theirPhoto in theirPhotos) {
        val myPhoto = myPhotos.first { it.photoId.id == theirPhoto.exchangedPhotoId.id }

        result += ReceivedPhotoResponseData(
          theirPhoto.photoId.id,
          myPhoto.photoName.name,
          theirPhoto.photoName.name,
          theirPhoto.lon,
          theirPhoto.lat,
          theirPhoto.uploadedOn
        )
      }

      return@dbQuery result
    }
  }

  suspend fun countFreshGalleryPhotosSince(time: Long): Int {
    return dbQuery(0) {
      return@dbQuery galleryPhotoDao.countFreshGalleryPhotosSince(time).awaitFirst()
    }
  }

  suspend fun countFreshReceivedPhotosSince(userUuid: UserUuid, time: Long): Int {
    return dbQuery(0) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery 0
      }

      return@dbQuery photosDao.countFreshUploadedPhotosSince(user.id, time)
    }
  }

  suspend fun countFreshUploadedPhotosSince(userUuid: UserUuid, time: Long): Int {
    return dbQuery(0) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery 0
      }

      return@dbQuery photosDao.countFreshExchangedPhotos(user.id, time)
    }
  }

  data class GalleryPhotoInfoDto(
    var galleryPhotoId: Long,
    var galleryPhotoName: String,
    var isFavourited: Boolean = false,
    var isReported: Boolean = false
  )

  data class GalleryPhotoDto(
    val photo: Photo,
    val galleryPhoto: GalleryPhoto
  )

  sealed class ReportPhotoResult {
    object Reported : ReportPhotoResult()
    object Unreported : ReportPhotoResult()
    object PhotoDoesNotExist : ReportPhotoResult()
    object Error : ReportPhotoResult()
  }

  sealed class FavouritePhotoResult {
    class Favourited(val count: Long) : FavouritePhotoResult()
    class Unfavourited(val count: Long) : FavouritePhotoResult()
    object PhotoDoesNotExist : FavouritePhotoResult()
    object Error : FavouritePhotoResult()
  }
}




















