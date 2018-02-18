package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.async

class PhotoInfoRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val photoInfoDao: PhotoInfoDao,
	private val photoInfoExchangeDao: PhotoInfoExchangeDao,
	private val mongoThreadPoolContext: ThreadPoolDispatcher
) {
	suspend fun save(photoInfo: PhotoInfo): PhotoInfo {
		return async(mongoThreadPoolContext) {
			photoInfo.photoId = mongoSequenceDao.getNextPhotoId()
			return@async photoInfoDao.save(photoInfo)
		}.await()
	}

	suspend fun find(userId: String, photoName: String): PhotoInfo {
		return async(mongoThreadPoolContext) {
			return@async photoInfoDao.find(userId, photoName)
		}.await()
	}

	suspend fun find(photoId: Long): PhotoInfo {
		return async(mongoThreadPoolContext) {
			return@async photoInfoDao.find(photoId)
		}.await()
	}

	suspend fun findOlderThan(time: Long): List<PhotoInfo> {
		return async(mongoThreadPoolContext) {
			return@async photoInfoDao.findOlderThan(time)
		}.await()
	}

	suspend fun countUserUploadedPhotos(userId: String): Long {
		return async(mongoThreadPoolContext) {
			val idList = photoInfoDao.findAllUserUploadedPhotoInfoIds(userId)
			if (idList.isEmpty()) {
				return@async 0L
			}

			return@async photoInfoExchangeDao.countAllUploadedByIdList(idList)
		}.await()
	}

	suspend fun countUserReceivedPhotos(userId: String): Long {
		return async(mongoThreadPoolContext) {
			val idList = photoInfoDao.findAllUserUploadedPhotoInfoIds(userId)
			if (idList.isEmpty()) {
				return@async 0L
			}

			return@async photoInfoExchangeDao.countAllReceivedByIdList(idList)
		}.await()
	}

	suspend fun updateSetPhotoSuccessfullyDelivered(photoName: String, userId: String, time: Long): Boolean {
		return async(mongoThreadPoolContext) {
			val photoInfo = photoInfoDao.find(userId, photoName)
			if (photoInfo.isEmpty()) {
				return@async false
			}

			val photoExchangeInfo = photoInfoExchangeDao.findById(photoInfo.exchangeId)
			if (photoExchangeInfo.isEmpty()) {
				return@async false
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

			return@async photoInfoExchangeDao.updateSetPhotoSuccessfullyDelivered(photoInfo.exchangeId, isUploader, time)
		}.await()
	}

	suspend fun updateSetExchangeInfoId(photoId: Long, exchangeId: Long): Boolean {
		return async(mongoThreadPoolContext) {
//			val photoInfo = photoInfoDao.find(photoId)
//			if (photoInfo.isEmpty()) {
//				return@async false
//			}
//
//			photoInfo.exchangeId = exchangeId
			return@async photoInfoDao.updateSetExchangeId(photoId, exchangeId)
		}.await()
	}

	suspend fun deleteUserById(userId: String): Boolean {
		return async(mongoThreadPoolContext) {
			return@async photoInfoDao.deleteUserById(userId)
		}.await()
	}

	suspend fun deleteAll(ids: List<Long>): Boolean {
		return async(mongoThreadPoolContext) {
			return@async photoInfoDao.deleteAll(ids)
		}.await()
	}
}




















