package com.kirakishou.photoexchange.database.pgsql.entity

import com.kirakishou.photoexchange.core.FavouritedPhoto
import com.kirakishou.photoexchange.core.FavouritedPhotoId
import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.database.pgsql.table.FavouritedPhotos
import org.jetbrains.exposed.sql.ResultRow

data class FavouritedPhotoEntity(
  val id: FavouritedPhotoId,
  val photoId: PhotoId,
  val userId: UserId
) {

  fun isEmpty() = id.isEmpty()

  fun toFavouritedPhoto(): FavouritedPhoto {
    return FavouritedPhoto(id, photoId, userId)
  }

  companion object {
    fun empty(): FavouritedPhotoEntity {
      return FavouritedPhotoEntity(
        FavouritedPhotoId.empty(),
        PhotoId.empty(),
        UserId.empty()
      )
    }

    fun fromResultRow(resultRow: ResultRow): FavouritedPhotoEntity {
      return FavouritedPhotoEntity(
        FavouritedPhotoId(resultRow[FavouritedPhotos.id]),
        PhotoId(resultRow[FavouritedPhotos.photoId]),
        UserId(resultRow[FavouritedPhotos.userId])
      )
    }
  }
}