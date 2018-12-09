package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.entity.FavouritedPhoto
import com.kirakishou.photoexchange.database.entity.GalleryPhoto
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.database.entity.ReportedPhoto
import com.kirakishou.photoexchange.exception.DatabaseTransactionException
import com.kirakishou.photoexchange.exception.ExchangeException
import com.kirakishou.photoexchange.service.DiskManipulationService
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.response.*
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.Mono

open class PhotoInfoRepository(
  private val template: ReactiveMongoTemplate,
  private val mongoSequenceDao: MongoSequenceDao,
  private val photoInfoDao: PhotoInfoDao,
  private val galleryPhotoDao: GalleryPhotoDao,
  private val favouritedPhotoDao: FavouritedPhotoDao,
  private val reportedPhotoDao: ReportedPhotoDao,
  private val userInfoDao: UserInfoDao,
  private val locationMapDao: LocationMapDao,
  private val generator: GeneratorService,
  private val diskManipulationService: DiskManipulationService
) : AbstractRepository() {
  private val mutex = Mutex()
  private val logger = LoggerFactory.getLogger(PhotoInfoRepository::class.java)

  suspend fun generatePhotoInfoName(): String {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        var photoName = ""

        while (true) {
          val generatedName = generator.generateNewPhotoName()
          if (!photoInfoDao.photoNameExists(generatedName).awaitFirst()) {
            photoName = generatedName
            break
          }
        }

        return@withLock photoName
      }
    }
  }

  suspend fun save(photoInfo: PhotoInfo): PhotoInfo {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        photoInfo.photoId = mongoSequenceDao.getNextPhotoId().awaitFirst()

        val savedPhotoInfo = photoInfoDao.save(photoInfo).awaitFirst()
        if (savedPhotoInfo.isEmpty()) {
          return@withLock savedPhotoInfo
        }

        if (photoInfo.isPublic) {
          val galleryPhotoId = mongoSequenceDao.getNextGalleryPhotoId().awaitFirst()
          val newGalleryPhoto = GalleryPhoto.create(
            galleryPhotoId,
            photoInfo.photoId,
            savedPhotoInfo.uploadedOn
          )

          val result = galleryPhotoDao.save(newGalleryPhoto).awaitFirst()
          if (!result) {
            photoInfoDao.deleteById(photoInfo.photoId).awaitFirst()
            return@withLock PhotoInfo.empty()
          }
        }

        return@withLock savedPhotoInfo
      }
    }
  }

  suspend fun findOneById(photoId: Long): PhotoInfo {
    return withContext(coroutineContext) {
      return@withContext photoInfoDao.findById(photoId).awaitFirst()
    }
  }

  suspend fun findOneByPhotoName(photoName: String): PhotoInfo {
    return withContext(coroutineContext) {
      return@withContext photoInfoDao.findByPhotoName(photoName).awaitFirst()
    }
  }

  suspend fun findAllIpHashesByUserId(userId: String): List<String> {
    return withContext(coroutineContext) {
      return@withContext photoInfoDao.findAllByUserId(userId).awaitFirst()
        .map { it.ipHash }
        .toSet()  //remove duplicates
        .toList()
    }
  }

  suspend fun findPageOfUploadedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<GetUploadedPhotosResponse.UploadedPhotoResponseData> {
    return withContext(coroutineContext) {
      val myPhotos = photoInfoDao.findPage(userId, lastUploadedOn, count).awaitFirst()

      val exchangeIds = myPhotos.map { it.exchangedPhotoId }
      val theirPhotos = photoInfoDao.findManyByIds(exchangeIds).awaitFirst()

      val result = mutableListOf<GetUploadedPhotosResponse.UploadedPhotoResponseData>()

      for (myPhoto in myPhotos) {
        val theirPhoto = theirPhotos.firstOrNull { it.photoId == myPhoto.exchangedPhotoId }
        val receiverInfo = theirPhoto
          ?.let { GetUploadedPhotosResponse.ReceiverInfoResponseData(it.photoName, it.lon, it.lat) }

        result += GetUploadedPhotosResponse.UploadedPhotoResponseData(
          myPhoto.photoId,
          myPhoto.photoName,
          myPhoto.lon,
          myPhoto.lat,
          receiverInfo,
          myPhoto.uploadedOn)
      }

      return@withContext result
    }
  }

  suspend fun findPageOfReceivedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<ReceivedPhotosResponse.ReceivedPhotoResponseData> {
    return withContext(coroutineContext) {
      val myPhotos = photoInfoDao.findPageOfExchangedPhotos(userId, lastUploadedOn, count).awaitFirst()
      val theirPhotoIds = myPhotos
        .map { it.exchangedPhotoId }

      val theirPhotos = photoInfoDao.findManyByIds(theirPhotoIds, PhotoInfoDao.SortOrder.Descending).awaitFirst()
      val result = mutableListOf<ReceivedPhotosResponse.ReceivedPhotoResponseData>()

      for (theirPhoto in theirPhotos) {
        val myPhoto = myPhotos.first { it.photoId == theirPhoto.exchangedPhotoId }

        result += ReceivedPhotosResponse.ReceivedPhotoResponseData(
          theirPhoto.photoId,
          myPhoto.photoName,
          theirPhoto.photoName,
          theirPhoto.lon,
          theirPhoto.lat,
          theirPhoto.uploadedOn
        )
      }

      return@withContext result
    }
  }

  suspend fun markAsDeletedPhotosUploadedEarlierThan(
    uploadedEarlierThan: Long,
    now: Long,
    count: Int
  ): Int {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val oldPhotos = photoInfoDao.findPhotosUploadedEarlierThan(uploadedEarlierThan, count).awaitFirst()
        val oldPhotosMap = oldPhotos.associateBy { it.photoId }
        val exchangedPhotosMap = oldPhotos.associateBy { it.exchangedPhotoId }

        val toBeUpdated = hashSetOf<Long>()

        for (photo in oldPhotosMap) {
          if (toBeUpdated.contains(photo.key)) {
            continue
          }

          val exchangedPhoto = exchangedPhotosMap[photo.key]
          if (exchangedPhoto == null) {
            continue
          }

          if (toBeUpdated.contains(exchangedPhoto.photoId)) {
            continue
          }

          if (photo.value.photoId != exchangedPhoto.exchangedPhotoId
            || photo.value.exchangedPhotoId != exchangedPhoto.photoId) {

            logger.debug("exchanged photos does not have each other's ids: " +
              "photo.value.photoId = ${photo.value.photoId}, " +
              "exchangedPhoto.exchangedPhotoId = ${exchangedPhoto.exchangedPhotoId}, " +
              "photo.value.exchangedPhotoId = ${photo.value.exchangedPhotoId}, " +
              "exchangedPhoto.photoId = ${exchangedPhoto.photoId}")
            continue
          }

          //if both of the photos were uploaded earlier than "uploadedEarlierThan" - mark them as deleted
          if (photo.value.uploadedOn < uploadedEarlierThan
            && exchangedPhoto.uploadedOn < uploadedEarlierThan) {

            toBeUpdated.add(photo.key)
            toBeUpdated.add(exchangedPhoto.photoId)
          }
        }

        if (!photoInfoDao.updateManySetDeletedOn(now, toBeUpdated.toList()).awaitFirst()) {
          return@withLock -1
        }

        return@withLock toBeUpdated.size
      }
    }
  }

  suspend fun cleanDatabaseAndPhotos(
    deletedEarlierThan: Long,
    photosPerQuery: Int
  ) {
    withContext(coroutineContext) {
      mutex.withLock {
        val photosToDelete = photoInfoDao.findDeletedEarlierThan(deletedEarlierThan, photosPerQuery).awaitFirst()
        if (photosToDelete.isEmpty()) {
          logger.debug("No photos to delete")
          return@withLock
        }

        logger.debug("Found ${photosToDelete.size} photos to delete")
        val photoFilesToDelete = mutableListOf<String>()

        for (photoInfo in photosToDelete) {
          logger.debug("Deleting ${photoInfo.photoName}")

          //TODO: probably should rewrite this to delete all photos in one transaction
          if (!deletePhotoInternalInTransaction(photoInfo)) {
            logger.error("Could not deletePhotoWithFile photo ${photoInfo.photoName}")
            continue
          }

          photoFilesToDelete += photoInfo.photoName
        }

        if (photoFilesToDelete.isNotEmpty()) {
          photoFilesToDelete.forEach { diskManipulationService.deleteAllPhotoFiles(it) }
        }
      }
    }
  }

  //TODO: rewrite to use transactions
  suspend fun tryDoExchange(userId: String, newUploadingPhoto: PhotoInfo): PhotoInfo {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val oldestPhoto = photoInfoDao.findOldestVacantPhoto(userId).awaitFirst()
        if (oldestPhoto.isEmpty()) {
          //no photos to exchange with, set newUploadingPhoto's exchangeId to PhotoInfo.EMPTY_PHOTO_ID
          //so it can be found by other's upon uploading
          if (!photoInfoDao.resetVacantPhoto(newUploadingPhoto.photoId).awaitFirst()) {
            throw RuntimeException("Something is wrong with the database. " +
              "Could not reset photo exchange id for photo with id (${oldestPhoto.photoId})")
          }

          return@withLock PhotoInfo.empty()
        }

        try {
          if (!photoInfoDao.updatePhotoSetReceiverId(oldestPhoto.photoId, newUploadingPhoto.photoId).awaitFirst()) {
            throw ExchangeException()
          }

          if (!photoInfoDao.updatePhotoSetReceiverId(newUploadingPhoto.photoId, oldestPhoto.photoId).awaitFirst()) {
            throw ExchangeException()
          }

          return@withLock oldestPhoto
            .copy(exchangedPhotoId = newUploadingPhoto.photoId)
        } catch (exception: ExchangeException) {
          if (!photoInfoDao.resetPhotoReceiverId(newUploadingPhoto.photoId).awaitFirst()) {
            throw RuntimeException("Something is wrong with the database. " +
              "Could not reset photo receiver id for photo with id (${newUploadingPhoto.photoId})")
          }

          if (!photoInfoDao.resetPhotoReceiverId(oldestPhoto.photoId).awaitFirst()) {
            throw RuntimeException("Something is wrong with the database. " +
              "Could not reset photo receiver id for photo with id (${oldestPhoto.photoId})")
          }

          if (!photoInfoDao.resetVacantPhoto(oldestPhoto.photoId).awaitFirst()) {
            throw RuntimeException("Something is wrong with the database. " +
              "Could not reset photo exchange id for photo with id (${oldestPhoto.photoId})")
          }

          return@withLock PhotoInfo.empty()
        }
      }
    }
  }

  suspend fun delete(userId: String, photoName: String): Boolean {
    return withContext(coroutineContext) {
      val photoInfo = photoInfoDao.findOneByUserIdAndPhotoName(userId, photoName).awaitFirst()
      return@withContext deletePhotoInternalInTransaction(photoInfo)
    }
  }

  suspend fun delete(photoInfo: PhotoInfo): Boolean {
    return withContext(coroutineContext) {
      return@withContext deletePhotoInternalInTransaction(photoInfo)
    }
  }

  private suspend fun deletePhotoInternalInTransaction(photoInfo: PhotoInfo): Boolean {
    if (photoInfo.isEmpty()) {
      return false
    }

    return mutex.withLock {
      return@withLock template.inTransaction().execute {
        return@execute mono {
          val results = mutableListOf<Mono<Boolean>>()
          results += photoInfoDao.deleteById(photoInfo.photoId)
            .doOnNext { result ->
              if (!result) {
                throw DatabaseTransactionException("photoInfoDao: Could not delete photo by id ${photoInfo.photoId}")
              }
            }
          results += favouritedPhotoDao.deleteByPhotoId(photoInfo.photoId)
            .doOnNext { result ->
              if (!result) {
                throw DatabaseTransactionException("favouritedPhotoDao: Could not delete photo by id ${photoInfo.photoId}")
              }
            }
          results += reportedPhotoDao.deleteByPhotoId(photoInfo.photoId)
            .doOnNext { result ->
              if (!result) {
                throw DatabaseTransactionException("reportedPhotoDao: Could not delete photo by id ${photoInfo.photoId}")
              }
            }
          results += locationMapDao.deleteById(photoInfo.photoId)
            .doOnNext { result ->
              if (!result) {
                throw DatabaseTransactionException("locationMapDao: Could not delete location map by id ${photoInfo.photoId}")
              }
            }
          results += galleryPhotoDao.deleteById(photoInfo.photoId)
            .doOnNext { result ->
              if (!result) {
                throw DatabaseTransactionException("galleryPhotoDao: Could not delete gallery photo by id ${photoInfo.photoId}")
              }
            }

          results.forEach { it.awaitFirst() }
          return@mono true
        }
      }
    }
      .single()
      .onErrorReturn(false)
      .awaitFirst()
  }

  suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoResult {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val photoId = photoInfoDao.getPhotoIdByName(photoName).awaitFirst()

        return@withLock if (favouritedPhotoDao.isPhotoFavourited(userId, photoId).awaitFirst()) {
          if (!favouritedPhotoDao.unfavouritePhoto(userId, photoId).awaitFirst()) {
            return@withLock FavouritePhotoResult.Error()
          }

          val favouritesCount = favouritedPhotoDao.countByPhotoId(photoId).awaitFirst()
          FavouritePhotoResult.Unfavourited(favouritesCount)
        } else {
          val id = mongoSequenceDao.getNextFavouritedPhotoId().awaitFirst()
          if (!favouritedPhotoDao.favouritePhoto(FavouritedPhoto.create(id, photoName, userId, photoId)).awaitFirst()) {
            return@withLock FavouritePhotoResult.Error()
          }

          val favouritesCount = favouritedPhotoDao.countByPhotoId(photoId).awaitFirst()
          FavouritePhotoResult.Favourited(favouritesCount)
        }
      }
    }
  }

  suspend fun reportPhoto(userId: String, photoName: String): ReportPhotoResult {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val photoId = photoInfoDao.getPhotoIdByName(photoName).awaitFirst()

        return@withLock if (reportedPhotoDao.isPhotoReported(userId, photoId).awaitFirst()) {
          if (!reportedPhotoDao.unreportPhoto(userId, photoId).awaitFirst()) {
            return@withLock ReportPhotoResult.Error()
          }

          ReportPhotoResult.Unreported()
        } else {

          val id = mongoSequenceDao.getNextReportedPhotoId().awaitFirst()
          if (!reportedPhotoDao.reportPhoto(ReportedPhoto.create(id, photoName, userId, photoId)).awaitFirst()) {
            return@withLock ReportPhotoResult.Error()
          }

          ReportPhotoResult.Reported()
        }
      }
    }
  }

  suspend fun findGalleryPhotos(lastUploadedOn: Long, count: Int): List<GalleryPhotosResponse.GalleryPhotoResponseData> {
    return withContext(coroutineContext) {
      val resultMap = linkedMapOf<Long, GalleryPhotoDto>()

      val pageOfGalleryPhotos = galleryPhotoDao.findPage(lastUploadedOn, count).awaitFirst()
      val photoIds = pageOfGalleryPhotos.map { it.photoId }

      val photoInfosDeferred = photoInfoDao.findManyByIds(photoIds, PhotoInfoDao.SortOrder.Descending)
      val favouritedPhotosMapDeferred = favouritedPhotoDao.findMany(photoIds)

      val photoInfos = photoInfosDeferred.awaitFirst()
      val favouritedPhotosMap = favouritedPhotosMapDeferred.awaitFirst().groupBy { it.photoId }

      for (photo in photoInfos) {
        val galleryPhoto = pageOfGalleryPhotos.first { it.photoId == photo.photoId }
        val favouritedPhotos = favouritedPhotosMap[photo.photoId] ?: emptyList()

        resultMap[photo.photoId] = GalleryPhotoDto(photo, galleryPhoto, favouritedPhotos.size.toLong())
      }

      return@withContext resultMap.values.map { (photoInfo, _, favouritesCount) ->
        GalleryPhotosResponse.GalleryPhotoResponseData(
          photoInfo.photoName,
          photoInfo.lon,
          photoInfo.lat,
          photoInfo.uploadedOn,
          favouritesCount
        )
      }
    }
  }

  suspend fun findGalleryPhotosInfo(
    userId: String,
    photoNames: List<String>
  ): List<GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData> {
    return withContext(coroutineContext) {
      val resultMap = linkedMapOf<Long, GalleryPhotoInfoDto>()

      val userFavouritedPhotosDeferred = favouritedPhotoDao.findManyByPhotoNameList(userId, photoNames)
      val userReportedPhotosDeferred = reportedPhotoDao.findManyByPhotoNameList(userId, photoNames)

      val userFavouritedPhotos = userFavouritedPhotosDeferred.awaitFirst()
      val userReportedPhotos = userReportedPhotosDeferred.awaitFirst()

      for (favouritedPhoto in userFavouritedPhotos) {
        resultMap.putIfAbsent(favouritedPhoto.photoId, GalleryPhotoInfoDto(favouritedPhoto.photoName))
        resultMap[favouritedPhoto.photoId]!!.isFavourited = true
      }

      for (reportedPhoto in userReportedPhotos) {
        resultMap.putIfAbsent(reportedPhoto.photoId, GalleryPhotoInfoDto(reportedPhoto.photoName))
        resultMap[reportedPhoto.photoId]!!.isReported = true
      }

      return@withContext resultMap.values.map { (galleryPhotoName, isFavourited, isReported) ->
        GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData(
          galleryPhotoName,
          isFavourited,
          isReported
        )
      }
    }
  }

  suspend fun findPhotosWithReceiverByPhotoNamesList(
    userId: String,
    photoNameList: List<String>
  ): List<ReceivedPhotosResponse.ReceivedPhotoResponseData> {
    return withContext(coroutineContext) {
      val myPhotos = photoInfoDao.findPhotosByNames(userId, photoNameList).awaitFirst()
      val theirPhotoIds = myPhotos.map { it.exchangedPhotoId }

      val theirPhotos = photoInfoDao.findManyByIds(theirPhotoIds).awaitFirst()
      val result = mutableListOf<ReceivedPhotosResponse.ReceivedPhotoResponseData>()

      for (theirPhoto in theirPhotos) {
        val myPhoto = myPhotos.first { it.photoId == theirPhoto.exchangedPhotoId }

        result += ReceivedPhotosResponse.ReceivedPhotoResponseData(
          theirPhoto.photoId,
          myPhoto.photoName,
          theirPhoto.photoName,
          theirPhoto.lon,
          theirPhoto.lat,
          theirPhoto.uploadedOn
        )
      }

      return@withContext result
    }
  }

  suspend fun updateSetLocationMapId(photoId: Long, locationMapId: Long): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock photoInfoDao.updateSetLocationMapId(photoId, locationMapId).awaitFirst()
      }
    }
  }

  suspend fun countFreshGalleryPhotosSince(time: Long): Int {
    return withContext(coroutineContext) {
      return@withContext galleryPhotoDao.countFreshGalleryPhotosSince(time).awaitFirst()
    }
  }

  suspend fun countFreshReceivedPhotosSince(userId: String, time: Long): Int {
    return withContext(coroutineContext) {
      return@withContext photoInfoDao.countFreshUploadedPhotosSince(userId, time).awaitFirst()
    }
  }

  suspend fun countFreshUploadedPhotosSince(userId: String, time: Long): Int {
    return withContext(coroutineContext) {
      return@withContext photoInfoDao.countFreshExchangedPhotos(userId, time).awaitFirst()
    }
  }

  data class GalleryPhotoInfoDto(
    var galleryPhotoName: String,
    var isFavourited: Boolean = false,
    var isReported: Boolean = false
  )

  data class GalleryPhotoDto(
    val photoInfo: PhotoInfo,
    val galleryPhoto: GalleryPhoto,
    val favouritesCount: Long
  )

  sealed class ReportPhotoResult {
    class Reported : ReportPhotoResult()
    class Unreported : ReportPhotoResult()
    class Error : ReportPhotoResult()
  }

  sealed class FavouritePhotoResult {
    class Favourited(val count: Long) : FavouritePhotoResult()
    class Unfavourited(val count: Long) : FavouritePhotoResult()
    class Error : FavouritePhotoResult()
  }
}




















