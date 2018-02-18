package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.async

class PhotoInfoExchangeRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val photoInfoDao: PhotoInfoDao,
	private val photoInfoExchangeDao: PhotoInfoExchangeDao,
	private val mongoThreadPoolContext: ThreadPoolDispatcher
) {
	suspend fun save(photoInfoExchange: PhotoInfoExchange): PhotoInfoExchange {
		return async(mongoThreadPoolContext) {
			photoInfoExchange.exchangeId = mongoSequenceDao.getNextPhotoExchangeId()
			return@async photoInfoExchangeDao.save(photoInfoExchange)
		}.await()
	}

	suspend fun findAllByIdList(ids: List<Long>): List<PhotoInfoExchange> {
		return async(mongoThreadPoolContext) {
			return@async photoInfoExchangeDao.findAllByIdList(ids)
		}.await()
	}

	suspend fun tryDoExchangeWithOldestPhoto(receiverPhotoId: Long): PhotoInfoExchange {
		return async(mongoThreadPoolContext) {
			val photoInfoExchange =  photoInfoExchangeDao.tryDoExchangeWithOldestPhoto(receiverPhotoId)
			if (!photoInfoExchange.isEmpty()) {
				//for some reason template.findAndModify returns old object (not updated)
				//so this is a temporary solution
				photoInfoExchange.receiverPhotoInfoId = receiverPhotoId
			}

			return@async photoInfoExchange
		}.await()
	}
}