package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.model.exception.ExchangeException
import com.kirakishou.photoexchange.model.net.response.received_photos.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.model.net.response.uploaded_photos.GetUploadedPhotosResponse
import com.kirakishou.photoexchange.model.repo.*
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

open class PhotoInfoRepository(
  private val mongoSequenceDao: MongoSequenceDao,
  private val photoInfoDao: PhotoInfoDao,
  private val photoInfoExchangeDao: PhotoInfoExchangeDao,
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

        val galleryPhotoId = mongoSequenceDao.getNextGalleryPhotoId().awaitFirst()
        val result = galleryPhotoDao.save(GalleryPhoto.create(galleryPhotoId, photoInfo.photoId,
          savedPhotoInfo.uploadedOn)).awaitFirst()

        if (!result) {
          photoInfoDao.deleteById(photoInfo.photoId).awaitFirst()
          return@withLock PhotoInfo.empty()
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

  suspend fun findPageOfUploadedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<GetUploadedPhotosResponse.UploadedPhoto> {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val uploadedPhotos = photoInfoDao.findPageOfUploadedPhotos(userId, lastUploadedOn, count).awaitFirst()
        val exchangeIdList = uploadedPhotos.map { it.exchangeId }
        val photoExchangeMap = photoInfoExchangeDao.findManyByIdList(exchangeIdList).awaitFirst()
          .associateBy { it.id }

        return@withLock uploadedPhotos.map { uploadedPhoto ->
          val photoInfoExchange = photoExchangeMap.getOrElse(uploadedPhoto.exchangeId, { PhotoInfoExchange.empty() })
          val hasReceiverInfo = photoInfoExchange
            .takeIf { !it.isEmpty() }?.receiverUserId?.isNotEmpty() ?: false

          GetUploadedPhotosResponse.UploadedPhoto(
            uploadedPhoto.photoId,
            uploadedPhoto.photoName,
            uploadedPhoto.lon,
            uploadedPhoto.lat,
            hasReceiverInfo,
            uploadedPhoto.uploadedOn)
        }
      }
    }
  }

  suspend fun findPageOfReceivedPhotos(
    userId: String,
    lastUploadedOn: Long,
    count: Int
  ): List<GetReceivedPhotosResponse.ReceivedPhoto> {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val receivedPhotos = photoInfoDao.findPageOfReceivedPhotos(userId, lastUploadedOn, count).awaitFirst()

        println()
        println()
        println()
        println()
        println()
        println()
        println()
        println()
        println()


        val resultList = mutableListOf<Pair<PhotoInfo, PhotoInfo>>()

        val uploaderUserIdList = receivedPhotos.map { it.receiverUserId }
        val exchangeIdList = receivedPhotos.map { it.exchangeId }

        val exchangedPhotoInfoList = photoInfoDao.findManyByUserIdAndExchangeId(uploaderUserIdList, exchangeIdList)
          .awaitFirst()

        val photoInfoMap = receivedPhotos.associateBy { it.exchangeId }
        val exchangedPhotoInfoMap = exchangedPhotoInfoList.associateBy { it.exchangeId }

        for (photoInfo in receivedPhotos) {
          if (!photoInfoMap.containsKey(photoInfo.exchangeId) || !exchangedPhotoInfoMap.containsKey(photoInfo.exchangeId)) {
            continue
          }

          resultList += Pair(
            photoInfoMap[photoInfo.exchangeId]!!,
            exchangedPhotoInfoMap[photoInfo.exchangeId]!!
          )
        }

        return@withLock resultList.map { photos ->
          GetReceivedPhotosResponse.ReceivedPhoto(
            photos.first.photoId,
            photos.second.photoName,
            photos.first.photoName,
            photos.first.lon,
            photos.first.lat
          )
        }
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

  suspend fun tryDoExchange(userId: String, newUploadingPhoto: PhotoInfo) {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val oldestPhoto = photoInfoDao.findOldestVacantPhoto(userId).awaitFirst()
        if (oldestPhoto.isEmpty()) {
          //no photos to exchange with, do nothing
          return@withLock
        }

        try {
          if (!photoInfoDao.updatePhotoSetReceiverId(oldestPhoto.photoId, newUploadingPhoto.photoId).awaitFirst()) {
            throw ExchangeException()
          }

          if (!photoInfoDao.updatePhotoSetReceiverId(newUploadingPhoto.photoId, oldestPhoto.photoId).awaitFirst()) {
            throw ExchangeException()
          }
        } catch (exception: ExchangeException) {
          if (!photoInfoDao.resetPhotoReceiverId(newUploadingPhoto.photoId).awaitFirst()) {
            throw RuntimeException("Something is wrong with the database")
          }

          if (!photoInfoDao.resetPhotoReceiverId(oldestPhoto.photoId).awaitFirst()) {
            throw RuntimeException("Something is wrong with the database")
          }
        }
      }
    }
  }

  suspend fun delete(userId: String, photoName: String, isUploader: Boolean = true): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val photoInfo = photoInfoDao.findOneByUserIdAndPhotoName(userId, photoName, isUploader).awaitFirst()
        return@withLock deletePhotoInternal(photoInfo)
      }
    }
  }

  suspend fun deleteAll(ids: List<Long>): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val photoInfoList = photoInfoDao.findManyByIds(ids).awaitFirst()
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
    results += photoInfoExchangeDao.deleteById(photoInfo.exchangeId)
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
    val exchangeIds = filteredPhotoInfoList.map { it.exchangeId }
    val results = mutableListOf<Mono<Boolean>>()

    results += photoInfoDao.deleteAll(photoIds)
    results += photoInfoExchangeDao.deleteAll(exchangeIds)
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
          if (!favouritedPhotoDao.favouritePhoto(FavouritedPhoto.create(id, userId, photoId)).awaitFirst()) {
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
          if (!reportedPhotoDao.reportPhoto(ReportedPhoto.create(id, userId, photoId)).awaitFirst()) {
            return@withLock ReportPhotoResult.Error()
          }

          ReportPhotoResult.Reported()
        }
      }
    }
  }

  suspend fun findGalleryPhotos(lastUploadedOn: Long, count: Int): LinkedHashMap<Long, GalleryPhotoDto> {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val resultMap = linkedMapOf<Long, GalleryPhotoDto>()

        val pageOfGalleryPhotos = galleryPhotoDao.findPaged(lastUploadedOn, count).awaitFirst()
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

        return@withLock resultMap
      }
    }
  }

  suspend fun findGalleryPhotosInfo(userId: String, galleryPhotoIdList: List<Long>): LinkedHashMap<Long, GalleryPhotoInfoDto> {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val resultMap = linkedMapOf<Long, GalleryPhotoInfoDto>()

        val userFavouritedPhotosDeffered = favouritedPhotoDao.findMany(userId, galleryPhotoIdList)
        val userReportedPhotosDeffered = reportedPhotoDao.findMany(userId, galleryPhotoIdList)

        val userFavouritedPhotos = userFavouritedPhotosDeffered.awaitFirst()
        val userReportedPhotos = userReportedPhotosDeffered.awaitFirst()

        for (favouritedPhoto in userFavouritedPhotos) {
          resultMap.putIfAbsent(favouritedPhoto.photoId, GalleryPhotoInfoDto(favouritedPhoto.photoId))
          resultMap[favouritedPhoto.photoId]!!.isFavourited = true
        }

        for (reportedPhoto in userReportedPhotos) {
          resultMap.putIfAbsent(reportedPhoto.photoId, GalleryPhotoInfoDto(reportedPhoto.photoId))
          resultMap[reportedPhoto.photoId]!!.isReported = true
        }

        return@withLock resultMap
      }
    }
  }

  suspend fun findPhotosWithReceiverByPhotoNamesList(userId: String, photoNameList: List<String>): List<Pair<PhotoInfo, PhotoInfo>> {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val photoInfoList = photoInfoDao.findManyByUserIdAndPhotoNamesWithReceiver(userId, photoNameList).awaitFirst()

        val resultList = mutableListOf<Pair<PhotoInfo, PhotoInfo>>()
        val uploaderUserIdList = photoInfoList.map { it.receiverUserId }
        val exchangeIdList = photoInfoList.map { it.exchangeId }

        val exchangedPhotoInfoList = photoInfoDao.findManyByUserIdAndExchangeId(uploaderUserIdList, exchangeIdList)
          .awaitFirst()

        val photoInfoMap = photoInfoList.associateBy { it.exchangeId }
        val exchangedPhotoInfoMap = exchangedPhotoInfoList.associateBy { it.exchangeId }

        if (photoInfoMap.isEmpty() || exchangedPhotoInfoMap.isEmpty()) {
          return@withLock emptyList<Pair<PhotoInfo, PhotoInfo>>()
        }

        for (photoInfo in photoInfoList) {
          if (!photoInfoMap.containsKey(photoInfo.exchangeId) || !exchangedPhotoInfoMap.containsKey(photoInfo.exchangeId)) {
            continue
          }

          resultList += Pair(
            photoInfoMap[photoInfo.exchangeId]!!,
            exchangedPhotoInfoMap[photoInfo.exchangeId]!!
          )
        }

        return@withLock resultList
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

  data class PhotoInfoWithLocation(
    var photoInfo: PhotoInfo,
    var lon: Double,
    var lat: Double
  )

  data class GalleryPhotoInfoDto(
    var galleryPhotoId: Long,
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




















