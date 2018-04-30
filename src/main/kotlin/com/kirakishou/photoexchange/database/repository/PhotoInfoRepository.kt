package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.model.repo.GalleryPhoto
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import com.kirakishou.photoexchange.service.ConcurrencyService
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class PhotoInfoRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val photoInfoDao: PhotoInfoDao,
	private val photoInfoExchangeDao: PhotoInfoExchangeDao,
	private val galleryPhotoDao: GalleryPhotoDao,
	private val concurrentService: ConcurrencyService
) {
	private val mutex = Mutex()

	suspend fun save(photoInfo: PhotoInfo): PhotoInfo {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				photoInfo.photoId = mongoSequenceDao.getNextPhotoId()

				val savedPhotoInfo = photoInfoDao.save(photoInfo)
				if (savedPhotoInfo.isEmpty()) {
					return@withLock savedPhotoInfo
				}

				val result = galleryPhotoDao.save(GalleryPhoto.create(photoInfo.photoId, savedPhotoInfo.uploadedOn))
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

	suspend fun findByExchangeIdAndUserIdAsync(userId: String, exchangeId: Long): Deferred<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findByExchangeIdAndUserId(userId, exchangeId)
		}
	}

	suspend fun findOlderThan(time: Long): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findOlderThan(time)
		}.await()
	}

	suspend fun countUserUploadedPhotos(userId: String): Deferred<Long> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.countAllUserUploadedPhotos(userId)
		}
	}

	suspend fun countUserReceivedPhotos(userId: String): Deferred<Long> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val idList = photoInfoDao.findAllPhotoIdsByUserId(userId)
				if (idList.isEmpty()) {
					return@withLock 0L
				}

				return@withLock photoInfoExchangeDao.countAllReceivedByIdList(idList)
			}
		}
	}

	suspend fun updateSetPhotoSuccessfullyDelivered(photoName: String, userId: String): UpdateSetPhotoDeliveredResult {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val photoInfo = photoInfoDao.find(userId, photoName)
				if (photoInfo.isEmpty()) {
					return@withLock UpdateSetPhotoDeliveredResult.PhotoInfoNotFound()
				}

				val photoExchangeInfo = photoInfoExchangeDao.findById(photoInfo.exchangeId)
				if (photoExchangeInfo.isEmpty()) {
					return@withLock UpdateSetPhotoDeliveredResult.PhotoInfoExchangeNotFound()
				}

				val isUploader = when {
					photoInfo.userId == photoExchangeInfo.uploaderUserId -> true
					photoInfo.userId == photoExchangeInfo.receiverUserId -> false
					else -> throw IllegalStateException("Neither of uploaderUserId and " +
						"receiverUserId equals to photoInfo.photoId " +
						"(photoId: ${photoInfo.photoId}, " +
						"uploaderUserId: ${photoExchangeInfo.uploaderUserId}, " +
						"receiverUserId: ${photoExchangeInfo.receiverUserId})")
				}

				if (!photoInfoExchangeDao.updateSetPhotoSuccessfullyDelivered(photoInfo.exchangeId, isUploader)) {
					return@withLock UpdateSetPhotoDeliveredResult.UpdateError()
				}

				return@withLock UpdateSetPhotoDeliveredResult.Ok()
			}
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
					val newPhotoInfoExchange = photoInfoExchangeDao.save(PhotoInfoExchange.create(newUploadingPhoto.userId))
					updateSetExchangeInfoId(newUploadingPhoto.photoId, newPhotoInfoExchange.id)
				} else {
					//there is a photo, update exchange request with info about our photo
					updateSetExchangeInfoId(newUploadingPhoto.photoId, photoInfoExchange.id)
				}
			}
		}
	}

	suspend fun deleteUserById(userId: String): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.deleteUserById(userId)
		}.await()
	}

	suspend fun deleteAll(ids: List<Long>): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.deleteAll(ids)
		}.await()
	}

	sealed class UpdateSetPhotoDeliveredResult {
		class Ok : UpdateSetPhotoDeliveredResult()
		class PhotoInfoNotFound : UpdateSetPhotoDeliveredResult()
		class PhotoInfoExchangeNotFound : UpdateSetPhotoDeliveredResult()
		class UpdateError : UpdateSetPhotoDeliveredResult()
	}
}




















