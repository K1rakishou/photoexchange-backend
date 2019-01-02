package com.kirakishou.photoexchange.core

import com.kirakishou.photoexchange.database.entity.LocationMapEntity

data class LocationMap(
  val id: LocationMapId,
  val photoId: PhotoId,
  val attemptsCount: Int,
  val mapStatus: LocationMapEntity.MapStatus,
  val nextAttemptTime: Long
)