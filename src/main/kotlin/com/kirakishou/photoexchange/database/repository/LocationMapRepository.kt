package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.LocationMapDao
import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.entity.LocationMap
import com.kirakishou.photoexchange.extensions.transactional
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.toFlux

open class LocationMapRepository(
  private val template: ReactiveMongoTemplate,
  private val mongoSequenceDao: MongoSequenceDao,
  private val locationMapDao: LocationMapDao,
  private val photoInfoDao: PhotoInfoDao,
  dispatcher: CoroutineDispatcher
) : AbstractRepository(dispatcher) {
  private val mutex = Mutex()
  private val logger = LoggerFactory.getLogger(LocationMapRepository::class.java)

  open suspend fun save(locationMap: LocationMap): LocationMap {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        locationMap.id = mongoSequenceDao.getNextLocationMapId().awaitFirst()
        return@withLock locationMapDao.save(locationMap).awaitFirst()
      }
    }
  }

  open suspend fun getOldest(count: Int, currentTime: Long): List<LocationMap> {
    return withContext(coroutineContext) {
      return@withContext locationMapDao.findOldest(count, currentTime).awaitFirst()
    }
  }

  open suspend fun setMapReady(photoId: Long, locationMapId: Long) {
    withContext(coroutineContext) {
      mutex.withLock {
        val result = template.transactional { txTemplate ->
          return@transactional locationMapDao.updateSetMapReady(photoId, txTemplate).toFlux()
            .flatMap { photoInfoDao.updateSetLocationMapId(photoId, locationMapId, txTemplate) }
        }.awaitFirst()

        if (!result) {
          logger.debug("Could not set map ready")
        }
      }
    }
  }

  open suspend fun setMapAnonymous(photoId: Long, locationMapId: Long) {
    withContext(coroutineContext) {
      mutex.withLock {
        val result = template.transactional { txTemplate ->
          return@transactional locationMapDao.updateSetMapAnonymous(photoId, txTemplate).toFlux()
            .flatMap { photoInfoDao.updateSetLocationMapId(photoId, locationMapId, txTemplate) }
        }.awaitFirst()

        if (!result) {
          logger.debug("Could not set map ready")
        }
      }
    }
  }

  open suspend fun setMapFailed(photoId: Long): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock locationMapDao.updateSetMapFailed(photoId).awaitFirst()
      }
    }
  }

  open suspend fun increaseAttemptsCountAndNextAttemptTime(photoId: Long, nextAttemptTime: Long): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock locationMapDao.increaseAttemptsCountAndNextAttemptTime(photoId, nextAttemptTime).awaitFirst()
      }
    }
  }
}