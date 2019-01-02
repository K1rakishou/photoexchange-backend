package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.core.DatabaseTransactionException
import com.kirakishou.photoexchange.core.LocationMap
import com.kirakishou.photoexchange.core.LocationMapId
import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.database.dao.LocationMapsDao
import com.kirakishou.photoexchange.database.dao.PhotosDao
import com.kirakishou.photoexchange.database.entity.LocationMapEntity
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.CoroutineDispatcher
import org.slf4j.LoggerFactory

open class LocationMapRepository(
  private val locationMapsDao: LocationMapsDao,
  private val photosDao: PhotosDao,
  dispatcher: CoroutineDispatcher
) : AbstractRepository(dispatcher) {
  private val logger = LoggerFactory.getLogger(LocationMapRepository::class.java)

  open suspend fun save(photoId: PhotoId): Boolean {
    return dbQuery(false) {
      return@dbQuery locationMapsDao.save(photoId)
    }
  }

  open suspend fun getOldest(count: Int, currentTime: Long): List<LocationMap> {
    return dbQuery(emptyList()) {
      return@dbQuery locationMapsDao.findOldest(count, currentTime)
        .map { it.toLocationMap() }
    }
  }

  open suspend fun setMapReady(photoId: PhotoId, locationMapId: LocationMapId) {
    dbQuery(null) {
      if (!locationMapsDao.updateSetMapStatus(photoId, LocationMapEntity.MapStatus.Ready)) {
        throw DatabaseTransactionException(
          "Could not update map with id (${locationMapId.id}) of photo (${photoId.id}) with status Ready"
        )
      }

      if (!photosDao.updateSetLocationMapId(photoId, locationMapId)) {
        throw DatabaseTransactionException(
          "Could not update photo with id (${photoId.id}) with locationMapId (${locationMapId.id})"
        )
      }
    }
  }

  open suspend fun setMapAnonymous(photoId: PhotoId, locationMapId: LocationMapId) {
    dbQuery {
      if (!locationMapsDao.updateSetMapStatus(photoId, LocationMapEntity.MapStatus.Anonymous)) {
        throw DatabaseTransactionException(
          "Could not update map with id (${locationMapId.id}) of photo (${photoId.id}) with status Anonymous"
        )
      }

      if (!photosDao.updateSetLocationMapId(photoId, locationMapId)) {
        throw DatabaseTransactionException(
          "Could not update photo with id (${photoId.id}) with locationMapId (${locationMapId.id})"
        )
      }
    }
  }

  open suspend fun setMapFailed(photoId: PhotoId, locationMapId: LocationMapId) {
    dbQuery {
      if (!locationMapsDao.updateSetMapStatus(photoId, LocationMapEntity.MapStatus.Failed)) {
        throw DatabaseTransactionException(
          "Could not update map with id (${locationMapId.id}) of photo (${photoId.id}) with status Failed"
        )
      }
    }
  }

  open suspend fun increaseAttemptsCountAndNextAttemptTime(photoId: PhotoId, repeatTimeDelta: Long) {
    dbQuery {
      val nextAttemptTime = TimeUtils.getTimeFast() + repeatTimeDelta

      if (!locationMapsDao.incrementAttemptsCount(photoId)) {
        throw DatabaseTransactionException(
          "Could not increase attempts count for photo with id (${photoId.id})"
        )
      }

      if (!locationMapsDao.updateNextAttemptTime(photoId, nextAttemptTime)) {
        throw DatabaseTransactionException(
          "Could not update next attempt time for photo with id (${photoId.id}) and time (${nextAttemptTime})"
        )
      }
    }
  }
}