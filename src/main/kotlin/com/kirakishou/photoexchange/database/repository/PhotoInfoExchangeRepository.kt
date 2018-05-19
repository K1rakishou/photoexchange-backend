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
	fun save(photoInfoExchange: PhotoInfoExchange): Mono<PhotoInfoExchange> {
		return mongoSequenceDao.getNextPhotoExchangeId()
			.flatMap { photoInfoExchangeDao.save(photoInfoExchange) }
	}

	fun findAllByIdList(ids: List<Long>): Mono<List<PhotoInfoExchange>> {
		return photoInfoExchangeDao.findManyByIdList(ids)
	}

	fun findById(exchangeId: Long): Mono<PhotoInfoExchange> {
		return photoInfoExchangeDao.findById(exchangeId)
	}

	fun findByIdAsync(exchangeId: Long): Mono<PhotoInfoExchange> {
		return photoInfoExchangeDao.findById(exchangeId)
	}
}