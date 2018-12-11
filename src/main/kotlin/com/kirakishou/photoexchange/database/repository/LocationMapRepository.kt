package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.LocationMapDao
import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.entity.LocationMap
import com.kirakishou.photoexchange.exception.CouldNotUpdateLocationId
import com.kirakishou.photoexchange.exception.CouldNotUpdateMapAnonymousFlag
import com.kirakishou.photoexchange.exception.CouldNotUpdateMapReadyFlag
import com.kirakishou.photoexchange.extensions.transactional
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

class LocationMapRepository(
	private val template: ReactiveMongoTemplate,
	private val mongoSequenceDao: MongoSequenceDao,
	private val locationMapDao: LocationMapDao,
	private val photoInfoDao: PhotoInfoDao
) : AbstractRepository() {
	private val mutex = Mutex()
	private val logger = LoggerFactory.getLogger(LocationMapRepository::class.java)

	suspend fun save(locationMap: LocationMap): LocationMap {
		return withContext(coroutineContext) {
			return@withContext mutex.withLock {
				locationMap.id = mongoSequenceDao.getNextLocationMapId().awaitFirst()
				return@withLock locationMapDao.save(locationMap).awaitFirst()
			}
		}
	}

	suspend fun getOldest(count: Int, currentTime: Long): List<LocationMap> {
		return withContext(coroutineContext) {
			return@withContext locationMapDao.findOldest(count, currentTime).awaitFirst()
		}
	}

	suspend fun setMapReady(photoId: Long, locationMapId: Long) {
		withContext(coroutineContext) {
			mutex.withLock {
				template.transactional(this) {
					if (!locationMapDao.updateSetMapReady(photoId).awaitFirst()) {
						throw CouldNotUpdateMapReadyFlag("Could not update map ready flag in the DB, " +
							"(photoId = $photoId, locationMapId = $locationMapId)")
					}

					if (!photoInfoDao.updateSetLocationMapId(photoId, locationMapId).awaitFirst()) {
						throw CouldNotUpdateLocationId("Could not update locationId in the DB, " +
							"(photoId = $photoId, locationMapId = $locationMapId)")
					}
				}
			}
		}
	}

	suspend fun setMapAnonymous(photoId: Long, locationMapId: Long) {
		withContext(coroutineContext) {
			mutex.withLock {
				template.transactional(this) {
					if (!locationMapDao.updateSetMapAnonymous(photoId).awaitFirst()) {
						throw CouldNotUpdateMapAnonymousFlag("Could not update map anonymous flag in the DB, " +
							"(photoId = $photoId, locationMapId = $locationMapId)")
					}

					if (!photoInfoDao.updateSetLocationMapId(photoId, locationMapId).awaitFirst()) {
						throw CouldNotUpdateLocationId("Could not update locationId in the DB, " +
							"(photoId = $photoId, locationMapId = $locationMapId)")
					}
				}
			}
		}
	}

	suspend fun setMapFailed(photoId: Long): Boolean {
    return withContext(coroutineContext) {
			return@withContext mutex.withLock {
				return@withLock locationMapDao.updateSetMapFailed(photoId).awaitFirst()
			}
		}
	}

	suspend fun increaseAttemptsCountAndNextAttemptTime(photoId: Long, nextAttemptTime: Long): Boolean {
    return withContext(coroutineContext) {
			return@withContext mutex.withLock {
				return@withLock locationMapDao.increaseAttemptsCountAndNextAttemptTime(photoId, nextAttemptTime).awaitFirst()
			}
		}
	}
}