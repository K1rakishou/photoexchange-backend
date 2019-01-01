package com.kirakishou.photoexchange.database.pgsql.entity

import com.kirakishou.photoexchange.core.GalleryPhoto
import com.kirakishou.photoexchange.core.GalleryPhotoId
import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.database.pgsql.table.GalleryPhotos
import org.jetbrains.exposed.sql.ResultRow

class GalleryPhotoEntity(
  val galleryPhotoId: GalleryPhotoId,
  val photoId: PhotoId,
  val uploadedOn: Long
) {

  fun isEmpty() = galleryPhotoId.isEmpty()

  fun toGalleryPhoto(): GalleryPhoto {
    return GalleryPhoto(
      galleryPhotoId,
      photoId,
      uploadedOn
    )
  }

  companion object {
    fun empty(): GalleryPhotoEntity {
      return GalleryPhotoEntity(
        GalleryPhotoId.empty(),
        PhotoId.empty(),
        -1L
      )
    }

    fun create(photoId: PhotoId, uploadedOn: Long): GalleryPhotoEntity {
      return GalleryPhotoEntity(
        GalleryPhotoId.empty(),
        photoId,
        uploadedOn
      )
    }

    fun fromResultRow(resultRow: ResultRow): GalleryPhotoEntity {
      return GalleryPhotoEntity(
        GalleryPhotoId(resultRow[GalleryPhotos.id]),
        PhotoId(resultRow[GalleryPhotos.photoId]),
        resultRow[GalleryPhotos.uploadedOn]
      )
    }
  }
}