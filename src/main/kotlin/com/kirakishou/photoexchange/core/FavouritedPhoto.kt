package com.kirakishou.photoexchange.core

data class FavouritedPhoto(
  val id: FavouritedPhotoId,
  val photoId: PhotoId,
  val userId: UserId
)