package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import reactor.core.publisher.Mono

class PhotoInfoExchangeRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val photoInfoExchangeDao: PhotoInfoExchangeDao,
	private val concurrentService: AbstractConcurrencyService
) {
	private val mutex = Mutex()

	suspend fun save(photoInfoExchange: PhotoInfoExchange): Mono<PhotoInfoExchange> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				return@withLock mongoSequenceDao.getNextPhotoExchangeId()
					.flatMap { photoInfoExchangeDao.save(photoInfoExchange) }
			}
		}.await()
	}

	suspend fun findMany(exchangeIdList: List<Long>): Mono<List<PhotoInfoExchange>> {
		return concurrentService.asyncMongo {
			return@asyncMongo photoInfoExchangeDao.findManyByIdList(exchangeIdList)
		}.await()
	}
}