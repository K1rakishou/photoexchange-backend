package com.kirakishou.photoexchange.core

data class ReportedPhoto(
  val reportedPhotoId: ReportedPhotoId,
  val photoId: PhotoId,
  val userId: UserId
)