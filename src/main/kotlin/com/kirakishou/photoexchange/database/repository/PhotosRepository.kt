package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import com.kirakishou.photoexchange.service.DiskManipulationService
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.CoroutineDispatcher
import net.response.data.*
import org.jetbrains.exposed.sql.Database
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.io.IOException

open class PhotosRepository(
  private val photosDao: PhotosDao,
  private val usersDao: UsersDao,
  private val galleryPhotosDao: GalleryPhotosDao,
  private val favouritedPhotosDao: FavouritedPhotosDao,
  private val reportedPhotosDao: ReportedPhotosDao,
  private val locationMapsDao: LocationMapsDao,
  private val generator: GeneratorService,
  private val diskManipulationService: DiskManipulationService,
  database: Database,
  dispatcher: CoroutineDispatcher
) : AbstractRepository(database, dispatcher) {
  private val logger = LoggerFactory.getLogger(PhotosRepository::class.java)

  private fun generatePhotoInfoName(): PhotoName {
    var photoName = PhotoName.empty()

    while (true) {
      val generatedName = generator.generateNewPhotoName()

      val found = photosDao.photoNameExists(generatedName)
      if (!found) {
        photoName = generatedName
        break
      }
    }

    return photoName
  }

  suspend fun save(userId: UserId, lon: Double, lat: Double, isPublic: Boolean, uploadedOn: DateTime, ipHash: IpHash): Photo {
    return dbQuery(Photo.empty()) {
      val photoName = generatePhotoInfoName()

      val photo = PhotoEntity.create(
        userId,
        photoName,
        isPublic,
        lon,
        lat,
        uploadedOn,
        ipHash
      )

      val savedPhoto = photosDao.save(photo)
      if (savedPhoto.isEmpty()) {
        return@dbQuery Photo.empty()
      }

      if (photo.isPublic) {
        val result = galleryPhotosDao.save(
          savedPhoto.photoId,
          savedPhoto.uploadedOn
        )

        if (!result) {
          throw DatabaseTransactionException(
            "Could not create gallery photo with photoId (${savedPhoto.photoId}) and uploadedOn (${savedPhoto.uploadedOn})"
          )
        }
      }

      return@dbQuery savedPhoto.toPhoto()
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

  suspend fun findAllPhotosByUserUuid(userUuid: UserUuid): List<Photo> {
    return dbQuery(emptyList()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery emptyList<Photo>()
      }

      return@dbQuery photosDao.findAllByUserId(user.userId)
        .map { photoEntity -> photoEntity.toPhoto() }
    }
  }

  suspend fun findPageOfUploadedPhotos(
    userUuid: UserUuid,
    lastUploadedOn: DateTime,
    count: Int
  ): List<UploadedPhotoResponseData> {
    return dbQuery(emptyList()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery emptyList<UploadedPhotoResponseData>()
      }

      val myPhotos = photosDao.findPageOfUploadedPhotos(user.userId, lastUploadedOn, count)
      val exchangeIds = myPhotos.map { it.exchangedPhotoId }
      val theirPhotos = photosDao.findManyByExchangedIdList(exchangeIds, false)

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
          myPhoto.uploadedOn.millis
        )
      }

      return@dbQuery result
    }
  }

  suspend fun findPageOfReceivedPhotos(
    userUuid: UserUuid,
    lastUploadedOn: DateTime,
    count: Int
  ): List<ReceivedPhotoResponseData> {
    return dbQuery(emptyList()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery emptyList<ReceivedPhotoResponseData>()
      }

      val myPhotos = photosDao.findPageOfReceivedPhotos(user.userId, lastUploadedOn, count)
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
          theirPhoto.uploadedOn.millis
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
    uploadedEarlierThanTime: DateTime,
    currentTime: DateTime,
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

        if (toBeUpdated.isEmpty()) {
          return@dbQuery 0
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
    deletedEarlierThanTime: DateTime,
    count: Int
  ) {
    dbQuery(Unit) {
      val photosToDelete = photosDao.findDeletedPhotos(deletedEarlierThanTime, count)
      if (photosToDelete.isEmpty()) {
        logger.debug("No photos to delete")
        return@dbQuery
      }

      logger.debug("Found ${photosToDelete.size} photos to delete")

      val photoIdListToDelete = photosToDelete.map { it.photoId }
      val photoNameListToDelete = photosToDelete.map { it.photoName }

      photosDao.deleteAll(photoIdListToDelete)
      logger.debug("Deleted (${photoNameListToDelete.joinToString(", ")})")

      if (photoNameListToDelete.isNotEmpty()) {
        photoNameListToDelete.forEach {
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

  open suspend fun tryDoExchange(userUuid: UserUuid, newUploadingPhoto: Photo): Photo {
    return dbQuery(Photo.empty()) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        logger.debug("Cannot continue photos exchange because sender does not have account with userUuid (${userUuid})")
        return@dbQuery Photo.empty()
      }

      val oldestPhoto = photosDao.findOldestEmptyPhoto(user.userId)
      if (oldestPhoto.isEmpty()) {
        this@PhotosRepository.logger.debug("No oldest empty photo in the database")
        return@dbQuery Photo.empty()
      }

      if (!photosDao.updateExchangeState(oldestPhoto.photoId, ExchangeState.Exchanging)) {
        throw DatabaseTransactionException(
          "Couldn't update exchange state for oldestPhoto with id (${oldestPhoto.photoId.id}) with Exchanging state"
        )
      }

      if (!photosDao.updateExchangeState(newUploadingPhoto.photoId, ExchangeState.Exchanging)) {
        throw DatabaseTransactionException(
          "Couldn't update exchange state for newUploadingPhoto with id (${oldestPhoto.photoId.id}) with Exchanging state"
        )
      }

      if (!photosDao.exchangePhotoWithOtherPhotoId(oldestPhoto.photoId, ExchangedPhotoId(newUploadingPhoto.photoId.id))) {
        throw DatabaseTransactionException(
          "Could not update receivedId for oldestPhoto with id (${oldestPhoto.photoId.id}) " +
            "with newUploadingPhoto.photoId.id (${newUploadingPhoto.photoId.id})"
        )
      }

      if (!photosDao.exchangePhotoWithOtherPhotoId(newUploadingPhoto.photoId, ExchangedPhotoId(oldestPhoto.photoId.id))) {
        throw DatabaseTransactionException(
          "Could not update receivedId for newUploadingPhoto with id (${newUploadingPhoto.photoId.id}) " +
            "with oldestPhoto.photoId.id (${oldestPhoto.photoId.id})"
        )
      }

      return@dbQuery oldestPhoto.toPhoto()
        .copy(
          exchangeState = ExchangeState.Exchanged,
          exchangedPhotoId = ExchangedPhotoId(newUploadingPhoto.photoId.id)
        )
    }
  }

  open suspend fun delete(photoId: PhotoId) {
    dbQuery(false) {
      photosDao.deleteById(photoId)
    }
  }

  suspend fun favouritePhoto(userUuid: UserUuid, photoName: PhotoName): FavouritePhotoResult {
    return dbQuery<FavouritePhotoResult>(FavouritePhotoResult.Error) {
      val userId = usersDao.getUser(userUuid).userId
      if (userId.isEmpty()) {
        return@dbQuery FavouritePhotoResult.UserDoesNotExist
      }

      val photo = photosDao.findByPhotoName(photoName)
      if (photo.isEmpty()) {
        return@dbQuery FavouritePhotoResult.PhotoDoesNotExist
      }

      return@dbQuery if (favouritedPhotosDao.isPhotoFavourited(photo.photoId, userId)) {
        favouritedPhotosDao.unfavouritePhoto(photo.photoId, userId)

        val favouritesCount = favouritedPhotosDao.countFavouritesByPhotoId(photo.photoId)
        FavouritePhotoResult.Unfavourited(favouritesCount)
      } else {
        if (!favouritedPhotosDao.favouritePhoto(photo.photoId, userId)) {
          return@dbQuery FavouritePhotoResult.Error
        }

        val favouritesCount = favouritedPhotosDao.countFavouritesByPhotoId(photo.photoId)
        FavouritePhotoResult.Favourited(favouritesCount)
      }
    }
  }

  suspend fun reportPhoto(userUuid: UserUuid, photoName: PhotoName): ReportPhotoResult {
    return dbQuery<ReportPhotoResult>(ReportPhotoResult.Error) {
      val userId = usersDao.getUser(userUuid).userId
      if (userId.isEmpty()) {
        return@dbQuery ReportPhotoResult.UserDoesNotExist
      }

      val photo = photosDao.findByPhotoName(photoName)
      if (photo.isEmpty()) {
        return@dbQuery ReportPhotoResult.PhotoDoesNotExist
      }

      return@dbQuery if (reportedPhotosDao.isPhotoReported(photo.photoId, userId)) {
        reportedPhotosDao.unreportPhoto(photo.photoId, userId)
        ReportPhotoResult.Unreported
      } else {
        if (!reportedPhotosDao.reportPhoto(photo.photoId, userId)) {
          return@dbQuery ReportPhotoResult.Error
        }

        ReportPhotoResult.Reported
      }
    }
  }

  suspend fun findGalleryPhotos(lastUploadedOn: DateTime, count: Int): List<GalleryPhotoResponseData> {
    return dbQuery(emptyList()) {
      val photoInfos = photosDao.findPageByGalleryPhotos(lastUploadedOn, count)

      return@dbQuery photoInfos.map { photoInfo ->
        GalleryPhotoResponseData(
          photoInfo.photoName.name,
          photoInfo.lon,
          photoInfo.lat,
          photoInfo.uploadedOn.millis
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

      val photos = photosDao.findManyByPhotoIdList(countMap.keys.map { PhotoId(it) }.toList(), false)
      for (photo in photos) {
        resultMap.putIfAbsent(photo.photoName.name, GalleryPhotoInfoDto(photo.photoId.id, photo.photoName.name))
      }

      val userFavouritedPhotos = favouritedPhotosDao.findManyFavouritedPhotos(user.userId, photoIdList)
      val userReportedPhotos = reportedPhotosDao.findManyReportedPhotos(user.userId, photoIdList)

      for (favouritedPhoto in userFavouritedPhotos) {
        val photoName = photoMap[favouritedPhoto.photoId]!!.photoName.name

        resultMap.putIfAbsent(photoName, GalleryPhotoInfoDto(favouritedPhoto.photoId.id, photoName))
        resultMap[photoName]!!.isFavourited = true
      }

      for (reportedPhoto in userReportedPhotos) {
        val photoName = photoMap[reportedPhoto.photoId]!!.photoName.name

        resultMap.putIfAbsent(photoName, GalleryPhotoInfoDto(reportedPhoto.photoId.id, photoName))
        resultMap[photoName]!!.isReported = true
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

      val myPhotos = photosDao.findPhotosByNames(user.userId, photoNameList)
      val theirPhotoIds = myPhotos.map { it.exchangedPhotoId }

      val theirPhotos = photosDao.findManyByExchangedIdList(theirPhotoIds, true)
      val result = mutableListOf<ReceivedPhotoResponseData>()

      for (theirPhoto in theirPhotos) {
        val myPhoto = myPhotos.first { it.photoId.id == theirPhoto.exchangedPhotoId.id }

        result += ReceivedPhotoResponseData(
          theirPhoto.photoId.id,
          myPhoto.photoName.name,
          theirPhoto.photoName.name,
          theirPhoto.lon,
          theirPhoto.lat,
          theirPhoto.uploadedOn.millis
        )
      }

      return@dbQuery result
    }
  }

  suspend fun countFreshGalleryPhotosSince(time: DateTime): Int {
    return dbQuery(0) {
      return@dbQuery galleryPhotosDao.countGalleryPhotosUploadedLaterThan(time)
    }
  }

  suspend fun countFreshReceivedPhotosSince(userUuid: UserUuid, time: DateTime): Int {
    return dbQuery(0) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery 0
      }

      return@dbQuery photosDao.countFreshUploadedPhotosSince(user.userId, time)
    }
  }

  suspend fun countFreshUploadedPhotosSince(userUuid: UserUuid, time: DateTime): Int {
    return dbQuery(0) {
      val user = usersDao.getUser(userUuid)
      if (user.isEmpty()) {
        return@dbQuery 0
      }

      return@dbQuery photosDao.countFreshExchangedPhotos(user.userId, time)
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
    object UserDoesNotExist : ReportPhotoResult()
    object Error : ReportPhotoResult()
  }

  sealed class FavouritePhotoResult {
    class Favourited(val count: Long) : FavouritePhotoResult()
    class Unfavourited(val count: Long) : FavouritePhotoResult()
    object PhotoDoesNotExist : FavouritePhotoResult()
    object UserDoesNotExist : FavouritePhotoResult()
    object Error : FavouritePhotoResult()
  }
}




















