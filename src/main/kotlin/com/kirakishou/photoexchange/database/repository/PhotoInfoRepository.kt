package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.model.repo.*
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class PhotoInfoRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val photoInfoDao: PhotoInfoDao,
	private val photoInfoExchangeDao: PhotoInfoExchangeDao,
	private val galleryPhotoDao: GalleryPhotoDao,
	private val favouritedPhotoDao: FavouritedPhotoDao,
	private val reportedPhotoDao: ReportedPhotoDao,
	private val userInfoRepository: UserInfoRepository,
	private val generator: GeneratorService,
	private val concurrentService: ConcurrencyService
) {
	private val mutex = Mutex()

	suspend fun generatePhotoInfoName(): String {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				var photoName = ""

				while (true) {
					val generatedName = generator.generateNewPhotoName()
					if (!photoInfoDao.photoNameExists(generatedName)) {
						photoName = generatedName
						break
					}
				}

				return@withLock photoName
			}
		}.await()
	}

	suspend fun save(photoInfo: PhotoInfo): PhotoInfo {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				photoInfo.photoId = mongoSequenceDao.getNextPhotoId()

				val savedPhotoInfo = photoInfoDao.save(photoInfo)
				if (savedPhotoInfo.isEmpty()) {
					return@withLock savedPhotoInfo
				}

				val galleryPhotoId = mongoSequenceDao.getNextGalleryPhotoId()
				val result = galleryPhotoDao.save(GalleryPhoto.create(galleryPhotoId, photoInfo.photoId, savedPhotoInfo.uploadedOn))

				if (!result) {
					photoInfoDao.deleteById(photoInfo.photoId)
					return@withLock PhotoInfo.empty()
				}

				return@withLock savedPhotoInfo
			}
		}.await()
	}

	suspend fun find(userId: String, photoName: String): PhotoInfo {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.find(userId, photoName)
		}.await()
	}

	suspend fun findMany(userId: String, photoNames: List<String>): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findMany(userId, photoNames)
		}.await()
	}

	suspend fun findMany(photoIdList: List<Long>): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findMany(photoIdList)
		}.await()
	}

	suspend fun findByExchangeIdAndUserIdAsync(userId: String, exchangeId: Long): Deferred<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findByExchangeIdAndUserId(userId, exchangeId)
		}
	}

	suspend fun findOlderThan(time: Long, maxCount: Int): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			val photos = photoInfoDao.findOlderThan(time, maxCount)
			val userInfos = userInfoRepository.findManyNotRegistered(photos.map { it.userId })

			val userIdsSet = userInfos.map { it.userId }.toSet()
			val resultList = mutableListOf<PhotoInfo>()

			for (photoInfo in photos) {
				if (!userIdsSet.contains(photoInfo.userId)) {
					continue
				}

				resultList += photoInfo
			}

			return@asyncMongo resultList
		}.await()
	}

	suspend fun updateSetExchangeInfoId(photoId: Long, exchangeId: Long): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.updateSetExchangeId(photoId, exchangeId)
		}.await()
	}

	suspend fun tryDoExchange(newUploadingPhoto: PhotoInfo) {
		concurrentService.asyncMongo {
			mutex.withLock {
				val photoInfoExchange = photoInfoExchangeDao.tryDoExchangeWithOldestPhoto(newUploadingPhoto.userId)
				if (photoInfoExchange.isEmpty()) {
					//there is no photo to do exchange with, create a new exchange request
					val photoExchangeId = mongoSequenceDao.getNextPhotoExchangeId()
					val newPhotoInfoExchange = photoInfoExchangeDao.save(PhotoInfoExchange.create(photoExchangeId, newUploadingPhoto.userId))
					updateSetExchangeInfoId(newUploadingPhoto.photoId, newPhotoInfoExchange.id)
				} else {
					//there is a photo, update exchange request with info about our photo
					updateSetExchangeInfoId(newUploadingPhoto.photoId, photoInfoExchange.id)
				}
			}
		}
	}

	suspend fun delete(userId: String, photoName: String): Boolean {
		return concurrentService.asyncMongo {
			val photoInfo = photoInfoDao.find(userId, photoName)
			return@asyncMongo deletePhotoInternal(photoInfo)
		}.await()
	}

	suspend fun deleteAll(ids: List<Long>): Boolean {
		return concurrentService.asyncMongo {
			val photoInfoList = photoInfoDao.findMany(ids)
			return@asyncMongo deletePhotosInternal(photoInfoList)
		}.await()
	}

	private suspend fun deletePhotoInternal(photoInfo: PhotoInfo): Boolean {
		return mutex.withLock {
			photoInfoDao.deleteById(photoInfo.photoId)
			photoInfoExchangeDao.deleteById(photoInfo.exchangeId)
			favouritedPhotoDao.deleteByPhotoId(photoInfo.photoId)
			reportedPhotoDao.deleteByPhotoId(photoInfo.photoId)

			return@withLock true
		}
	}

	private suspend fun deletePhotosInternal(photoInfoList: List<PhotoInfo>): Boolean {
		return mutex.withLock {
			val photoIds = photoInfoList.map { it.photoId }
			val exchangeIds = photoInfoList.map { it.exchangeId }

			photoInfoDao.deleteAll(photoIds)
			photoInfoExchangeDao.deleteAll(exchangeIds)
			favouritedPhotoDao.deleteAll(photoIds)
			reportedPhotoDao.deleteAll(photoIds)

			return@withLock true
		}
	}

	suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoResult {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val photoId = photoInfoDao.getPhotoIdByName(photoName)

				return@withLock if (favouritedPhotoDao.isPhotoFavourited(userId, photoId)) {
					if (!favouritedPhotoDao.unfavouritePhoto(userId, photoId)) {
						return@withLock FavouritePhotoResult.Error()
					}

					val photoInfo = photoInfoDao.changePhotoFavouritesCount(photoName, false)
					if (photoInfo.isEmpty()) {
						return@withLock FavouritePhotoResult.Error()
					}

					FavouritePhotoResult.Unfavourited(photoInfo.favouritesCount)
				} else {
					val id = mongoSequenceDao.getNextFavouritedPhotoId()
					if (!favouritedPhotoDao.favouritePhoto(FavouritedPhoto.create(id, userId, photoId))) {
						return@withLock FavouritePhotoResult.Error()
					}

					val photoInfo = photoInfoDao.changePhotoFavouritesCount(photoName, true)
					if (photoInfo.isEmpty()) {
						return@withLock FavouritePhotoResult.Error()
					}

					FavouritePhotoResult.Favourited(photoInfo.favouritesCount)
				}
			}
		}.await()
	}

	suspend fun reportPhoto(userId: String, photoName: String): ReportPhotoResult {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val photoId = photoInfoDao.getPhotoIdByName(photoName)

				return@withLock if (reportedPhotoDao.isPhotoReported(userId, photoId)) {
					if (!reportedPhotoDao.unreportPhoto(userId, photoId)) {
						return@withLock ReportPhotoResult.Error()
					}

					if (!photoInfoDao.changePhotoReportsCount(photoName, false)) {
						return@withLock ReportPhotoResult.Error()
					}

					ReportPhotoResult.Unreported()
				} else {

					val id = mongoSequenceDao.getNextReportedPhotoId()
					if (!reportedPhotoDao.reportPhoto(ReportedPhoto.create(id, userId, photoId))) {
						return@withLock ReportPhotoResult.Error()
					}

					if (!photoInfoDao.changePhotoReportsCount(photoName, true)) {
						return@withLock ReportPhotoResult.Error()
					}

					ReportPhotoResult.Reported()
				}
			}
		}.await()
	}

	suspend fun findGalleryPhotosByIds(galleryPhotoIdList: List<Long>): LinkedHashMap<Long, GalleryPhotoDto> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val resultMap = linkedMapOf<Long, GalleryPhotoDto>()

				val galleryPhotos = galleryPhotoDao.findManyByIdList(galleryPhotoIdList)
				val photoInfos = photoInfoDao.findMany(galleryPhotos.map { it.photoId })

				for (photo in photoInfos) {
					val galleryPhoto = galleryPhotos.first { it.photoId == photo.photoId }
					resultMap[photo.photoId] = GalleryPhotoDto(photo, galleryPhoto)
				}

				return@withLock resultMap
			}
		}.await()
	}

	suspend fun findGalleryPhotosInfo(userId: String, galleryPhotoIdList: List<Long>): LinkedHashMap<Long, GalleryPhotoInfoDto> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val resultMap = linkedMapOf<Long, GalleryPhotoInfoDto>()

				val userFavouritedPhotos = favouritedPhotoDao.findMany(userId, galleryPhotoIdList)
				val userReportedPhotos = reportedPhotoDao.findMany(userId, galleryPhotoIdList)

				for (favouritedPhoto in userFavouritedPhotos) {
					resultMap.putIfAbsent(favouritedPhoto.id, GalleryPhotoInfoDto(favouritedPhoto.photoId))
					resultMap[favouritedPhoto.id]!!.isFavourited = true
				}

				for (reportedPhoto in userReportedPhotos) {
					resultMap.putIfAbsent(reportedPhoto.id, GalleryPhotoInfoDto(reportedPhoto.photoId))
					resultMap[reportedPhoto.id]!!.isReported = true
				}

				return@withLock resultMap
			}
		}.await()
	}

	data class GalleryPhotoInfoDto(
		var galleryPhotoId: Long,
		var isFavourited: Boolean = false,
		var isReported: Boolean = false
	)

	data class GalleryPhotoDto(
		val photoInfo: PhotoInfo,
		val galleryPhoto: GalleryPhoto
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

	sealed class UpdateSetPhotoDeliveredResult {
		class Ok : UpdateSetPhotoDeliveredResult()
		class PhotoInfoNotFound : UpdateSetPhotoDeliveredResult()
		class PhotoInfoExchangeNotFound : UpdateSetPhotoDeliveredResult()
		class UpdateError : UpdateSetPhotoDeliveredResult()
	}
}




















