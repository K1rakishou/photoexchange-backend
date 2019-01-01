package com.kirakishou.photoexchange.database.pgsql.dao

import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.database.pgsql.entity.LocationMapEntity
import com.kirakishou.photoexchange.database.pgsql.table.LocationMaps
import org.jetbrains.exposed.sql.*

open class LocationMapsDao {

  open fun save(photoId: PhotoId): Boolean {
    val id = LocationMaps.insert {
      it[this.photoId] = photoId.id
    } get LocationMaps.id

    return id != null
  }

  open fun findOldest(count: Int, currentTime: Long): List<LocationMapEntity> {
    return LocationMaps.select {
      withNextAttemptTimeEarlierThan(currentTime) and
        withMapStatus(LocationMapEntity.MapStatus.Empty)
    }
      .orderBy(LocationMaps.id, true)
      .limit(count)
      .map { resultRow -> LocationMapEntity.fromResultRow(resultRow) }
  }

  open fun updateSetMapStatus(photoId: PhotoId, status: LocationMapEntity.MapStatus): Boolean {
    return LocationMaps.update({ withPhotoId(photoId) }) {
      it[LocationMaps.mapStatus] = status.value
    } == 1
  }

  open fun incrementAttemptsCount(photoId: PhotoId): Boolean {
    return LocationMaps.update({ withPhotoId(photoId) }) {
      with(SqlExpressionBuilder) {
        it.update(LocationMaps.attemptsCount, LocationMaps.attemptsCount + 1)
      }
    } == 1
  }

  open fun updateNextAttemptTime(photoId: PhotoId, nextAttemptTime: Long): Boolean {
    return LocationMaps.update({ withPhotoId(photoId) }) {
      it[LocationMaps.nextAttemptTime] = nextAttemptTime
    } == 1
  }

  open fun deleteById(photoId: PhotoId) {
    LocationMaps.deleteWhere {
      withPhotoId(photoId)
    }
  }

  /**
   * For test purposes
   * */

  open fun testFindAll(): List<LocationMapEntity> {
    return LocationMaps.selectAll()
      .map { resultRow -> LocationMapEntity.fromResultRow(resultRow) }
  }

  /**
   * LocationMap's next attempt must be less than "currentTime"
   * */
  private fun SqlExpressionBuilder.withNextAttemptTimeEarlierThan(currentTime: Long): Op<Boolean> {
    return LocationMaps.nextAttemptTime.less(currentTime)
  }

  /**
   * LocationMap must have this status
   * */
  private fun SqlExpressionBuilder.withMapStatus(status: LocationMapEntity.MapStatus): Op<Boolean> {
    return LocationMaps.mapStatus.eq(status.value)
  }

  /**
   * LocationMap must have this photoId
   * */
  private fun SqlExpressionBuilder.withPhotoId(photoId: PhotoId): Op<Boolean> {
    return LocationMaps.photoId.eq(photoId.id)
  }
}