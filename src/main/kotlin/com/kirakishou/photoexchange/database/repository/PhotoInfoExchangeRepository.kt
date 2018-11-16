package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import reactor.core.publisher.Mono

class PhotoInfoExchangeRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val photoInfoExchangeDao: PhotoInfoExchangeDao
) : AbstractRepository() {
	private val mutex = Mutex()

	suspend fun save(photoInfoExchange: PhotoInfoExchange): Mono<PhotoInfoExchange> {
		return withContext(coroutineContext) {
			return@withContext mutex.withLock {
				return@withLock mongoSequenceDao.getNextPhotoExchangeId()
					.flatMap { photoInfoExchangeDao.save(photoInfoExchange) }
			}
		}
	}

	suspend fun findMany(exchangeIdList: List<Long>): Mono<List<PhotoInfoExchange>> {
		return withContext(coroutineContext) {
			return@withContext photoInfoExchangeDao.findManyByIdList(exchangeIdList)
		}
	}
}