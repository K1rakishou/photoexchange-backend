package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.ConcurrencyService

class PhotoInfoRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val photoInfoDao: PhotoInfoDao,
	private val photoInfoExchangeDao: PhotoInfoExchangeDao,
	private val concurrentService: ConcurrencyService
) {
	suspend fun save(photoInfo: PhotoInfo): PhotoInfo {
		return concurrentService.asyncMongo {
			photoInfo.photoId = mongoSequenceDao.getNextPhotoId()
			return@asyncMongo photoInfoDao.save(photoInfo)
		}.await()
	}

	suspend fun find(userId: String, photoName: String): PhotoInfo {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.find(userId, photoName)
		}.await()
	}

	suspend fun find(photoId: Long): PhotoInfo {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.find(photoId)
		}.await()
	}

	suspend fun findOlderThan(time: Long): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.findOlderThan(time)
		}.await()
	}

	suspend fun countUserUploadedPhotos(userId: String): Long {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.countAllUserUploadedPhotos(userId)
		}.await()
	}

	suspend fun countUserReceivedPhotos(userId: String): Long {
		return concurrentService.asyncMongo {
			val idList = photoInfoDao.findAllUserUploadedPhotoInfoIds(userId)
			if (idList.isEmpty()) {
				return@asyncMongo 0L
			}

			return@asyncMongo photoInfoExchangeDao.countAllReceivedByIdList(idList)
		}.await()
	}

	suspend fun updateSetPhotoSuccessfullyDelivered(photoName: String, userId: String, time: Long): Boolean {
		return concurrentService.asyncMongo {
			val photoInfo = photoInfoDao.find(userId, photoName)
			if (photoInfo.isEmpty()) {
				return@asyncMongo false
			}

			val photoExchangeInfo = photoInfoExchangeDao.findById(photoInfo.exchangeId)
			if (photoExchangeInfo.isEmpty()) {
				return@asyncMongo false
			}

			val isUploader = when {
				photoInfo.photoId == photoExchangeInfo.uploaderPhotoInfoId -> true
				photoInfo.photoId == photoExchangeInfo.receiverPhotoInfoId -> false
				else -> throw IllegalStateException("Neither of uploaderPhotoInfoId and " +
					"receiverPhotoInfoId equals to photoInfo.photoId " +
					"(photoId: ${photoInfo.photoId}, " +
					"uploaderPhotoInfoId: ${photoExchangeInfo.uploaderPhotoInfoId}, " +
					"receiverPhotoInfoId: ${photoExchangeInfo.receiverPhotoInfoId})")
			}

			return@asyncMongo photoInfoExchangeDao.updateSetPhotoSuccessfullyDelivered(photoInfo.exchangeId, isUploader, time)
		}.await()
	}

	suspend fun updateSetExchangeInfoId(photoId: Long, exchangeId: Long): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoDao.updateSetExchangeId(photoId, exchangeId)
		}.await()
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
}




















