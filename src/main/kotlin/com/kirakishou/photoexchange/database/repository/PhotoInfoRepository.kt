package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.entity.FavouritedPhoto
import com.kirakishou.photoexchange.database.entity.GalleryPhoto
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.database.entity.ReportedPhoto
import com.kirakishou.photoexchange.exception.ExchangeException
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.response.*
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

open class PhotoInfoRepository(
  private val mongoSequenceDao: MongoSequenceDao,
  private val photoInfoDao: PhotoInfoDao,
  private val galleryPhotoDao: GalleryPhotoDao,
  private val favouritedPhotoDao: FavouritedPhotoDao,
  private val reportedPhotoDao: ReportedPhotoDao,
  private val userInfoDao: UserInfoDao,
  private val locationMapDao: LocationMapDao,
  private val generator: GeneratorService
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
      return@withContext mutex.withLock {
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

        return@withLock result
      }
    }
  }

  suspend fun findPageOfReceivedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<ReceivedPhotosResponse.ReceivedPhotoResponseData> {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
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

        return@withLock result
      }
    }
  }

  suspend fun findOlderThan(time: Long): List<PhotoInfo> {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock photoInfoDao.findOlderThan(time).awaitFirst()
      }
    }
  }

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
      return@withContext mutex.withLock {
        val photoInfo = photoInfoDao.findOneByUserIdAndPhotoName(userId, photoName).awaitFirst()
        return@withLock deletePhotoInternal(photoInfo)
      }
    }
  }

  suspend fun delete(photoInfo: PhotoInfo): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock deletePhotoInternal(photoInfo)
      }
    }
  }

  suspend fun cleanUp(ids: List<Long>): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val photoInfoList = photoInfoDao.findPhotosToDeleteByIds(ids).awaitFirst()
        return@withLock deletePhotosInternal(photoInfoList)
      }
    }
  }

  private suspend fun deletePhotoInternal(photoInfo: PhotoInfo): Boolean {
    if (photoInfo.isEmpty()) {
      return false
    }

    val results = mutableListOf<Mono<Boolean>>()
    results += photoInfoDao.deleteById(photoInfo.photoId)
    results += favouritedPhotoDao.deleteByPhotoId(photoInfo.photoId)
    results += reportedPhotoDao.deleteByPhotoId(photoInfo.photoId)
    results += locationMapDao.deleteById(photoInfo.photoId)
    results += galleryPhotoDao.deleteById(photoInfo.photoId)

    return results.map { it.awaitFirst() }
      .any { !it }
  }

  private suspend fun deletePhotosInternal(photoInfoList: List<PhotoInfo>): Boolean {
    val filteredPhotoInfoList = photoInfoList.filter { !it.isEmpty() }
    if (filteredPhotoInfoList.isEmpty()) {
      return false
    }

    val photoIds = filteredPhotoInfoList.map { it.photoId }
    val results = mutableListOf<Mono<Boolean>>()

    results += photoInfoDao.deleteAll(photoIds)
    results += favouritedPhotoDao.deleteAll(photoIds)
    results += reportedPhotoDao.deleteAll(photoIds)
    results += locationMapDao.deleteAll(photoIds)
    results += galleryPhotoDao.deleteAll(photoIds)

    return results.map { it.awaitFirst() }
      .any { !it }
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
      return@withContext mutex.withLock {
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

        return@withLock resultMap.values.map { (photoInfo, _, favouritesCount) ->
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
  }

  suspend fun findGalleryPhotosInfo(
    userId: String,
    photoNames: List<String>
  ): List<GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData> {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
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

        return@withLock resultMap.values.map { (galleryPhotoName, isFavourited, isReported) ->
          GalleryPhotoInfoResponse.GalleryPhotosInfoResponseData(
            galleryPhotoName,
            isFavourited,
            isReported
          )
        }
      }
    }
  }

  suspend fun findPhotosWithReceiverByPhotoNamesList(
    userId: String,
    photoNameList: List<String>
  ): List<ReceivedPhotosResponse.ReceivedPhotoResponseData> {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
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

        return@withLock result
      }
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
      return@withContext mutex.withLock {
        return@withLock galleryPhotoDao.countFreshGalleryPhotosSince(time).awaitFirst()
      }
    }
  }

  suspend fun countFreshReceivedPhotosSince(userId: String, time: Long): Int {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock photoInfoDao.countFreshUploadedPhotosSince(userId, time).awaitFirst()
      }
    }
  }

  suspend fun countFreshUploadedPhotosSince(userId: String, time: Long): Int {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock photoInfoDao.countFreshExchangedPhotos(userId, time).awaitFirst()
      }
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




















