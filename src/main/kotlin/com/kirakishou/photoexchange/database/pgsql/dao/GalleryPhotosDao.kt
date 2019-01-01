package com.kirakishou.photoexchange.database.pgsql.dao

import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.database.pgsql.entity.GalleryPhotoEntity
import com.kirakishou.photoexchange.database.pgsql.table.GalleryPhotos
import org.jetbrains.exposed.sql.*

open class GalleryPhotosDao {

  open fun save(photoId: PhotoId, uploadedOn: Long): Boolean {
    val id = GalleryPhotos.insert {
      it[GalleryPhotos.photoId] = photoId.id
      it[GalleryPhotos.uploadedOn] = uploadedOn
    } get GalleryPhotos.id

    return id != null
  }

  open fun findPage(lastUploadedOn: Long, count: Int): List<GalleryPhotoEntity> {
    return GalleryPhotos.select {
      uploadedEarlierThan(lastUploadedOn)
    }
      .orderBy(GalleryPhotos.uploadedOn, false)
      .limit(count)
      .map { resultRow -> GalleryPhotoEntity.fromResultRow(resultRow) }
  }

  open fun countGalleryPhotosUploadedLaterThan(currentTime: Long): Int {
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

  private fun SqlExpressionBuilder.withPhotoId(photoId: PhotoId): Op<Boolean> {
    return GalleryPhotos.photoId.eq(photoId.id)
  }

  private fun SqlExpressionBuilder.uploadedEarlierThan(lastUploadedOn: Long): Op<Boolean> {
    return GalleryPhotos.uploadedOn.less(lastUploadedOn)
  }

  private fun SqlExpressionBuilder.uploadedLaterThan(currentTime: Long): Op<Boolean> {
    return GalleryPhotos.uploadedOn.greater(currentTime)
  }
}