package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.model.repo.*
import com.kirakishou.photoexchange.service.GeneratorService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
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
	private val concurrentService: AbstractConcurrencyService
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

	suspend fun findMany(userId: String, photoNames: List<String>): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findMany(userId, photoNames)
		}.await()
	}

	suspend fun findManyUploadedPhotos(userId: String, photoIds: List<Long>): List<PhotoInfoWithLocation> {
		return concurrentService.asyncMongo {
			val photoInfos = photoInfoDao.findManyByIds(userId, photoIds)
			val exhangeIds = photoInfos.map { it.exchangeId }

			val otherPhotos = photoInfoDao.findManyPhotosByUserIdAndExchangeId(userId, exhangeIds)
			val otherPhotosMap = mutableMapOf<Long, PhotoInfo>()

			for (otherPhoto in otherPhotos) {
				otherPhotosMap[otherPhoto.exchangeId] = otherPhoto
			}

			return@asyncMongo photoInfos
				.map { PhotoInfoWithLocation(it, otherPhotosMap[it.exchangeId]?.lon ?: 0.0, otherPhotosMap[it.exchangeId]?.lat ?: 0.0) }
		}.await()
	}

	suspend fun findByExchangeIdAndUserId(userId: String, exchangeId: Long): PhotoInfo {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findByExchangeIdAndUserId(userId, exchangeId)
		}.await()
	}

	suspend fun findOlderThan(time: Long, maxCount: Int): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			val photos = photoInfoDao.findOlderThan(time, maxCount)
			val userInfos = userInfoRepository.findManyNotRegistered(photos.map { it.uploaderUserId })

			val userIdsSet = userInfos.map { it.userId }.toSet()
			val resultList = mutableListOf<PhotoInfo>()

			for (photoInfo in photos) {
				if (!userIdsSet.contains(photoInfo.uploaderUserId)) {
					continue
				}

				resultList += photoInfo
			}

			return@asyncMongo resultList
		}.await()
	}

	suspend fun findPaged(userId: String, lastId: Long, count: Int): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findPaged(userId, lastId, count)
		}.await()
	}

	suspend fun tryDoExchange(newUploadingPhoto: PhotoInfo): Boolean {
		return concurrentService.asyncMongo {
			mutex.withLock {
				val photoInfoExchange = photoInfoExchangeDao.tryDoExchangeWithOldestPhoto(newUploadingPhoto.photoId)

				val result = photoInfoExchange.isEmpty().let { isPhotoInfoExchangeEmpty ->
					if (isPhotoInfoExchangeEmpty) {
						val newPhotoExchangeResult = kotlin.run {
							//there is no photo to do exchange with, create a new exchange request
							val photoExchangeId = mongoSequenceDao.getNextPhotoExchangeId()
							val newPhotoInfoExchange = photoInfoExchangeDao.save(PhotoInfoExchange.create(photoExchangeId, newUploadingPhoto.photoId))
							if (newPhotoInfoExchange.isEmpty()) {
								return@run false
							}

							return@run photoInfoDao.updateSetExchangeId(newUploadingPhoto.photoId, newPhotoInfoExchange.id)
						}

						if (!newPhotoExchangeResult) {
							//TODO: error handling
						}

						return@let newPhotoExchangeResult
					} else {
						val existingPhotoExchangeResult = kotlin.run {
							//there is a photo, update exchange request with info about our photo
							if (!photoInfoDao.updateSetExchangeId(newUploadingPhoto.photoId, photoInfoExchange.id)) {
								return@run false
							}

							val ids = arrayListOf(photoInfoExchange.uploaderPhotoId, photoInfoExchange.receiverPhotoId)
							val photos = photoInfoDao.findMany(ids, PhotoInfoDao.SortOrder.Unsorted)
							if (photos.size != ids.size) {
								return@run false
							}

							if (!photoInfoDao.updateSetReceiverId(photoInfoExchange.uploaderPhotoId, photos.last().uploaderUserId)) {
								return@run false
							}

							return@run photoInfoDao.updateSetReceiverId(photoInfoExchange.receiverPhotoId, photos.first().uploaderUserId)
						}

						if (!existingPhotoExchangeResult) {
							//TODO: error handling
						}

						return@let existingPhotoExchangeResult
					}

				}

				return@withLock result
			}
		}.await()
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

					val favouritesCount = favouritedPhotoDao.countByPhotoId(photoId)
					FavouritePhotoResult.Unfavourited(favouritesCount)
				} else {
					val id = mongoSequenceDao.getNextFavouritedPhotoId()
					if (!favouritedPhotoDao.favouritePhoto(FavouritedPhoto.create(id, userId, photoId))) {
						return@withLock FavouritePhotoResult.Error()
					}

					val favouritesCount = favouritedPhotoDao.countByPhotoId(photoId)
					FavouritePhotoResult.Favourited(favouritesCount)
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

					ReportPhotoResult.Unreported()
				} else {

					val id = mongoSequenceDao.getNextReportedPhotoId()
					if (!reportedPhotoDao.reportPhoto(ReportedPhoto.create(id, userId, photoId))) {
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
				val photoIds = galleryPhotos.map { it.photoId }

				val photoInfos = photoInfoDao.findMany(photoIds)
				val favouritedPhotosMap = favouritedPhotoDao.findMany(photoIds).groupBy { it.photoId }

				for (photo in photoInfos) {
					val galleryPhoto = galleryPhotos.first { it.photoId == photo.photoId }
					val favouritedPhotos = favouritedPhotosMap[photo.photoId] ?: emptyList()

					resultMap[photo.photoId] = GalleryPhotoDto(photo, galleryPhoto, favouritedPhotos.size.toLong())
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




















