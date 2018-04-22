package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import com.kirakishou.photoexchange.service.ConcurrencyService

class PhotoInfoExchangeRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val photoInfoDao: PhotoInfoDao,
	private val photoInfoExchangeDao: PhotoInfoExchangeDao,
	private val concurrentService: ConcurrencyService
) {
	suspend fun save(photoInfoExchange: PhotoInfoExchange): PhotoInfoExchange {
		return concurrentService.asyncMongo {
			photoInfoExchange.id = mongoSequenceDao.getNextPhotoExchangeId()
			return@asyncMongo photoInfoExchangeDao.save(photoInfoExchange)
		}.await()
	}

	suspend fun findAllByIdList(ids: List<Long>): List<PhotoInfoExchange> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoExchangeDao.findAllByIdList(ids)
		}.await()
	}

	suspend fun tryDoExchangeWithOldestPhoto(receiverUserId: String): PhotoInfoExchange {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoExchangeDao.tryDoExchangeWithOldestPhoto(receiverUserId)
		}.await()
	}

	suspend fun findById(exchangeId: Long): PhotoInfoExchange {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoExchangeDao.findById(exchangeId)
		}.await()
	}
}