package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import reactor.core.publisher.Mono

class PhotoInfoExchangeRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val photoInfoDao: PhotoInfoDao,
	private val photoInfoExchangeDao: PhotoInfoExchangeDao,
	private val concurrentService: ConcurrencyService
) {
	suspend fun save(photoInfoExchange: PhotoInfoExchange): Mono<PhotoInfoExchange> {
		return concurrentService.asyncMongo {
			return@asyncMongo mongoSequenceDao.getNextPhotoExchangeId()
				.flatMap { photoInfoExchangeDao.save(photoInfoExchange) }
		}.await()
	}
}