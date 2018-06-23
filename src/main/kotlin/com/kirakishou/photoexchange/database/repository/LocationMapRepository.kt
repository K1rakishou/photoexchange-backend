package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.LocationMapDao
import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.model.repo.LocationMap
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.slf4j.LoggerFactory

class LocationMapRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val locationMapDao: LocationMapDao,
	private val concurrentService: AbstractConcurrencyService
) {
	private val mutex = Mutex()
	private val logger = LoggerFactory.getLogger(LocationMapRepository::class.java)

	suspend fun save(locationMap: LocationMap): LocationMap {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				locationMap.id = mongoSequenceDao.getNextLocationMapId().awaitFirst()
				return@withLock locationMapDao.save(locationMap).awaitFirst()
			}
		}.await()
	}

	suspend fun getOldest(count: Int): List<LocationMap> {
		return concurrentService.asyncMongo {
			return@asyncMongo locationMapDao.findOldest(count).awaitFirst()
		}.await()
	}

	suspend fun setMapReady(photoId: Long): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				return@withLock locationMapDao.updateSetMapReady(photoId).awaitFirst()
			}
		}.await()
	}

	suspend fun setMapFailed(photoId: Long): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				return@withLock locationMapDao.updateSetMapFailed(photoId).awaitFirst()
			}
		}.await()
	}

	suspend fun increaseAttemptsCount(photoId: Long): Boolean {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				return@withLock locationMapDao.increaseAttemptsCount(photoId).awaitFirst()
			}
		}.await()
	}
}