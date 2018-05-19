package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import kotlinx.coroutines.experimental.Deferred

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
			return@asyncMongo photoInfoExchangeDao.findManyByIdList(ids)
		}.await()
	}

	suspend fun findById(exchangeId: Long): PhotoInfoExchange {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoExchangeDao.findById(exchangeId)
		}.await()
	}

	suspend fun findByIdAsync(exchangeId: Long): Deferred<PhotoInfoExchange> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoExchangeDao.findById(exchangeId)
		}
	}
}