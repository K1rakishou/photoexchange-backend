package com.kirakishou.photoexchange.database.pgsql.entity

import com.kirakishou.photoexchange.core.LocationMap
import com.kirakishou.photoexchange.core.LocationMapId
import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.database.pgsql.table.LocationMaps
import org.jetbrains.exposed.sql.ResultRow

data class LocationMapEntity(
  val locationMapId: LocationMapId,
  val photoId: PhotoId,
  val attemptsCount: Int,
  val mapStatus: MapStatus,
  val nextAttemptTime: Long
) {

  fun isEmpty() = locationMapId.isEmpty()

  fun toLocationMap(): LocationMap {
    return LocationMap(
      locationMapId,
      photoId,
      attemptsCount,
      mapStatus,
      nextAttemptTime
    )
  }

  companion object {
    fun empty(): LocationMapEntity {
      return LocationMapEntity(LocationMapId.empty(), PhotoId.empty(), 0, MapStatus.Empty, 0L)
    }

    fun create(photoId: PhotoId): LocationMapEntity {
      return LocationMapEntity(LocationMapId.empty(), photoId, 0, MapStatus.Empty, 0L)
    }

    fun fromResultRow(resultRow: ResultRow): LocationMapEntity {
      return LocationMapEntity(
        LocationMapId(resultRow[LocationMaps.id]),
        PhotoId(resultRow[LocationMaps.photoId]),
        resultRow[LocationMaps.attemptsCount],
        MapStatus.fromInt(resultRow[LocationMaps.mapStatus]),
        resultRow[LocationMaps.nextAttemptTime]
      )
    }
  }

  enum class MapStatus(val value: Int) {
    Empty(0),
    Ready(1),
    Anonymous(2),
    Failed(3);

    companion object {
      fun fromInt(value: Int): MapStatus {
        return MapStatus.values().first { it.value == value }
      }
    }
  }
}