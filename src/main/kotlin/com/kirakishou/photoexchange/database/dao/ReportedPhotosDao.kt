package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.database.entity.ReportedPhotoEntity
import com.kirakishou.photoexchange.database.table.ReportedPhotos
import org.jetbrains.exposed.sql.*

open class ReportedPhotosDao {

  open fun reportPhoto(photoId: PhotoId, userId: UserId): Boolean {
    val id = ReportedPhotos.insert {
      it[ReportedPhotos.photoId] = photoId.id
      it[ReportedPhotos.userId] = userId.id
    } get ReportedPhotos.id

    return id != null
  }

  open fun unreportPhoto(photoId: PhotoId, userId: UserId) {
    ReportedPhotos.deleteWhere {
      withPhotoId(photoId) and
        withUserId(userId)
    }
  }

  open fun findManyReportedPhotos(userId: UserId, photoIdList: List<PhotoId>): List<ReportedPhotoEntity> {
    return ReportedPhotos.select {
      withUserId(userId) and
        withPhotoIdIn(photoIdList)
    }
      .limit(photoIdList.size)
      .map { resultRow -> ReportedPhotoEntity.fromResultRow(resultRow) }
  }

  open fun isPhotoReported(photoId: PhotoId, userId: UserId): Boolean {
    return ReportedPhotos.select {
      withPhotoId(photoId) and
        withUserId(userId)
    }
      .firstOrNull()
      ?.let { true } ?: false
  }

  open fun deleteAllFavouritesByPhotoId(photoId: PhotoId) {
    ReportedPhotos.deleteWhere {
      withPhotoId(photoId)
    }
  }

  /**
   * For test purposes
   * */

  open fun testFindAll(): List<ReportedPhotoEntity> {
    return ReportedPhotos.selectAll()
      .map { resultRow -> ReportedPhotoEntity.fromResultRow(resultRow) }
  }

  /**
   * ReportedPhoto must have one of the ids from the list
   * */
  private fun SqlExpressionBuilder.withPhotoIdIn(photoIdList: List<PhotoId>): Op<Boolean> {
    return ReportedPhotos.photoId.inList(photoIdList.map { it.id })
  }

  /**
   * ReportedPhoto must have this photoId
   * */
  private fun SqlExpressionBuilder.withPhotoId(photoId: PhotoId): Op<Boolean> {
    return ReportedPhotos.photoId.eq(photoId.id)
  }

  /**
   * ReportedPhoto must have this userId
   * */
  private fun SqlExpressionBuilder.withUserId(userId: UserId): Op<Boolean> {
    return ReportedPhotos.userId.eq(userId.id)
  }
}