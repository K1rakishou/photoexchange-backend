package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.model.repo.*
import com.kirakishou.photoexchange.service.GeneratorService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
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
	private val generator: GeneratorService,
	private val concurrentService: AbstractConcurrencyService
) {
	private val mutex = Mutex()
	private val logger = LoggerFactory.getLogger(PhotoInfoRepository::class.java)

	suspend fun generatePhotoInfoName(): String {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
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
		}.await()
	}

	suspend fun save(photoInfo: PhotoInfo): PhotoInfo {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
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
		}.await()
	}

	suspend fun findOneById(photoId: Long): PhotoInfo {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findById(photoId).awaitFirst()
		}.await()
	}

	suspend fun findManyPhotosFromUploader(userId: String, photoIds: List<Long>): List<PhotoInfoWithLocation> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val photoInfos = photoInfoDao.findManyByUserIdAndPhotoIds(userId, photoIds, true).awaitFirst()
				return@withLock photoInfos
					.map { PhotoInfoWithLocation(it, it.lon, it.lat) }
			}
		}.await()
	}

	suspend fun findOlderThan(time: Long): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				return@withLock photoInfoDao.findOlderThan(time).awaitFirst()
			}
		}.await()
	}

	suspend fun findUploadedPhotosPaged(userId: String, lastId: Long, count: Int): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findManyPagedFromUploader(userId, lastId, count).awaitFirst()
		}.await()
	}

	suspend fun findReceivedPhotosPaged(userId: String, lastId: Long, count: Int): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findManyPagedFromReceiver(userId, lastId, count).awaitFirst()
		}.await()
	}

	suspend fun tryDoExchange(newUploadingPhoto: PhotoInfo): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val photoInfoExchange = photoInfoExchangeDao.tryDoExchangeWithOldestPhoto(
					newUploadingPhoto.photoId, newUploadingPhoto.uploaderUserId
				).awaitFirst()

				return@withLock photoInfoExchange.isEmpty().let { isPhotoInfoExchangeEmpty ->
					if (isPhotoInfoExchangeEmpty) {
						var newPhotoInfoExchange = PhotoInfoExchange.empty()

						val newPhotoExchangeResult = kotlin.run {
							//there is no photo to do exchange with, create a new exchange request
							val photoExchangeId = mongoSequenceDao.getNextPhotoExchangeId().awaitFirst()
							newPhotoInfoExchange = photoInfoExchangeDao.save(PhotoInfoExchange.create(photoExchangeId,
								newUploadingPhoto.photoId, newUploadingPhoto.uploaderUserId)
							).awaitFirst()

							if (newPhotoInfoExchange.isEmpty()) {
								return@run false
							}

							return@run photoInfoDao.updateSetExchangeId(newUploadingPhoto.photoId,
								newPhotoInfoExchange.id).awaitFirst()
						}

						if (!newPhotoExchangeResult) {
							if (!newPhotoInfoExchange.isEmpty()) {
								if (!photoInfoExchangeDao.deleteById(newPhotoInfoExchange.id).awaitFirst()) {
									logger.error("ERROR! Could not delete photoInfoExchange with id " +
										"${newPhotoInfoExchange.id} after a failure to do photos exchange")
								}
							}
						}

						return@let newPhotoExchangeResult
					} else {
						val photos = mutableListOf<PhotoInfo>()
						val existingPhotoExchangeResult = kotlin.run {
							//there is a photo, update exchange request with info about our photo
							if (!photoInfoDao.updateSetExchangeId(newUploadingPhoto.photoId,
									photoInfoExchange.id).awaitFirst()) {
								return@run false
							}

							val ids = arrayListOf(photoInfoExchange.uploaderPhotoId, photoInfoExchange.receiverPhotoId)
							photos += photoInfoDao.findManyByIds(ids).awaitFirst()
							if (photos.size != ids.size) {
								return@run false
							}

							if (!photoInfoDao.updateSetReceiverId(photoInfoExchange.uploaderPhotoId,
									photos.last().uploaderUserId).awaitFirst()) {
								return@run false
							}

							if (!photoInfoDao.updateSetUploadedOn(photoInfoExchange.uploaderPhotoId,
									photos.last().uploadedOn).awaitFirst()) {
								return@run false
							}

							return@run photoInfoDao.updateSetReceiverId(photoInfoExchange.receiverPhotoId,
								photos.first().uploaderUserId).awaitFirst()
						}

						if (!existingPhotoExchangeResult) {
							val photoInfo1 = photos.first()
							val photoInfo2 = photos.last()

							val updateResults = mutableListOf<Mono<Boolean>>()

							updateResults += photoInfoDao.updateResetReceivedUserId(photoInfo1.photoId)
							updateResults += photoInfoDao.updateResetExchangeId(photoInfo2.photoId)
							updateResults += photoInfoExchangeDao.updateResetReceiverInfo(photoInfoExchange.id)

							val isAllResultsOk = updateResults
								.map { it.awaitFirst() }
								.any { !it }

							if (!isAllResultsOk) {
								logger.error("Could not restore receiver info after unsuccessful attempt in photos exchange")
							}
						}

						return@let existingPhotoExchangeResult
					}

				}
			}
		}.await()
	}

	suspend fun delete(userId: String, photoName: String, isUploader: Boolean = true): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val photoInfo = photoInfoDao.findOneByUserIdAndPhotoName(userId, photoName, isUploader).awaitFirst()
				return@withLock deletePhotoInternal(photoInfo)
			}
		}.await()
	}

	suspend fun deleteAll(ids: List<Long>): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val photoInfoList = photoInfoDao.findManyByIds(ids).awaitFirst()
				return@withLock deletePhotosInternal(photoInfoList)
			}
		}.await()
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

		return results.map { it.awaitFirst() }
			.any { !it }
	}

	suspend fun favouritePhoto(userId: String, photoName: String): FavouritePhotoResult {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
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
		}.await()
	}

	suspend fun reportPhoto(userId: String, photoName: String): ReportPhotoResult {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
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
		}.await()
	}

	suspend fun findGalleryPhotosByIds(galleryPhotoIdList: List<Long>): LinkedHashMap<Long, GalleryPhotoDto> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val resultMap = linkedMapOf<Long, GalleryPhotoDto>()

				val galleryPhotos = galleryPhotoDao.findManyByIdList(galleryPhotoIdList).awaitFirst()
				val photoIds = galleryPhotos.map { it.photoId }

				val photoInfosDeferred = photoInfoDao.findManyByIds(photoIds, PhotoInfoDao.SortOrder.Descending)
				val favouritedPhotosMapDeferred = favouritedPhotoDao.findMany(photoIds)

				val photoInfos = photoInfosDeferred.awaitFirst()
				val favouritedPhotosMap = favouritedPhotosMapDeferred.awaitFirst().groupBy { it.photoId }

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
		}.await()
	}

	suspend fun findPhotosWithReceiverByPhotoNamesList(userId: String, photoNameList: List<String>): List<Pair<PhotoInfo, PhotoInfo>> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
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
		}.await()
	}

	suspend fun findPhotosWithReceiverByPhotoIdsList(userId: String, photoIdList: List<Long>): List<Pair<PhotoInfo, PhotoInfo>> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val photoInfoList = photoInfoDao.findManyByUserIdAndPhotoIds(userId, photoIdList, false).awaitFirst()
				val resultList = mutableListOf<Pair<PhotoInfo, PhotoInfo>>()

				val uploaderUserIdList = photoInfoList.map { it.receiverUserId }
				val exchangeIdList = photoInfoList.map { it.exchangeId }

				val exchangedPhotoInfoList = photoInfoDao.findManyByUserIdAndExchangeId(uploaderUserIdList, exchangeIdList)
					.awaitFirst()

				val photoInfoMap = photoInfoList.associateBy { it.exchangeId }
				val exchangedPhotoInfoMap = exchangedPhotoInfoList.associateBy { it.exchangeId }

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
		}.await()
	}

	suspend fun updateSetLocationMapId(photoId: Long, locationMapId: Long): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				return@withLock photoInfoDao.updateSetLocationMapId(photoId, locationMapId).awaitFirst()
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




















