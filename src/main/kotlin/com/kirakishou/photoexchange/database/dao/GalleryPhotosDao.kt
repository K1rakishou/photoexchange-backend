package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.database.entity.GalleryPhotoEntity
import com.kirakishou.photoexchange.database.table.GalleryPhotos
import org.jetbrains.exposed.sql.*
import org.joda.time.DateTime

open class GalleryPhotosDao {

  open fun save(photoId: PhotoId, uploadedOn: DateTime): Boolean {
    val id = GalleryPhotos.insert {
      it[GalleryPhotos.photoId] = photoId.id
      it[GalleryPhotos.uploadedOn] = uploadedOn
    } get GalleryPhotos.id

    return id != null
  }

  open fun findPage(lastUploadedOn: DateTime, count: Int): List<GalleryPhotoEntity> {
    return GalleryPhotos.select {
      uploadedEarlierThan(lastUploadedOn)
    }
      .orderBy(GalleryPhotos.id, false)
      .limit(count)
      .map { resultRow -> GalleryPhotoEntity.fromResultRow(resultRow) }
  }

  open fun countGalleryPhotosUploadedLaterThan(currentTime: DateTime): Int {
    return GalleryPhotos.select {
      uploadedLaterThan(currentTime)
    }.count()
  }

  open fun deleteByPhotoId(photoId: PhotoId) {
    GalleryPhotos.deleteWhere {
      withPhotoId(photoId)
    }
  }

  /**
   * For test purposes
   * */

  open fun testFindAll(): List<GalleryPhotoEntity> {
    return GalleryPhotos.selectAll()
      .map { resultRow -> GalleryPhotoEntity.fromResultRow(resultRow) }
  }

  /**
   * Gallery photo must have this photoId
   * */
  private fun SqlExpressionBuilder.withPhotoId(photoId: PhotoId): Op<Boolean> {
    return GalleryPhotos.photoId.eq(photoId.id)
  }

  /**
   * Gallery photo must be uploaded earlier than "lastUploadedOn"
   * */
  private fun SqlExpressionBuilder.uploadedEarlierThan(lastUploadedOn: DateTime): Op<Boolean> {
    return GalleryPhotos.uploadedOn.less(lastUploadedOn)
  }

  /**
   * Gallery photo must be uploaded later than "currentTime"
   * */
  private fun SqlExpressionBuilder.uploadedLaterThan(currentTime: DateTime): Op<Boolean> {
    return GalleryPhotos.uploadedOn.greater(currentTime)
  }
}