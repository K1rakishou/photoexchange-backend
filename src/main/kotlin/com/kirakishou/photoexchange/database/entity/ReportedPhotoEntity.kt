package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.core.ReportedPhotoId
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.database.table.ReportedPhotos
import org.jetbrains.exposed.sql.ResultRow

data class ReportedPhotoEntity(
  val reportedPhotoId: ReportedPhotoId,
  val photoId: PhotoId,
  val userId: UserId
) {

  fun isEmpty() = reportedPhotoId.isEmpty()

  companion object {
    fun empty(): ReportedPhotoEntity {
      return ReportedPhotoEntity(
        ReportedPhotoId.empty(),
        PhotoId.empty(),
        UserId.empty()
      )
    }

    fun create(photoId: PhotoId, userId: UserId): ReportedPhotoEntity {
      return ReportedPhotoEntity(
        ReportedPhotoId.empty(),
        photoId,
        userId
      )
    }

    fun fromResultRow(resultRow: ResultRow): ReportedPhotoEntity {
      return ReportedPhotoEntity(
        ReportedPhotoId(resultRow[ReportedPhotos.id]),
        PhotoId(resultRow[ReportedPhotos.photoId]),
        UserId(resultRow[ReportedPhotos.userId])
      )
    }
  }

}