package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.database.entity.FavouritedPhotoEntity
import com.kirakishou.photoexchange.database.table.FavouritedPhotos
import org.jetbrains.exposed.sql.*

open class FavouritedPhotosDao {

  open fun favouritePhoto(photoId: PhotoId, userId: UserId): Boolean {
    val id = FavouritedPhotos.insert {
      it[FavouritedPhotos.photoId] = photoId.id
      it[FavouritedPhotos.userId] = userId.id
    } get FavouritedPhotos.id

    return id != null
  }

  open fun unfavouritePhoto(photoId: PhotoId, userId: UserId) {
    FavouritedPhotos.deleteWhere {
      withPhotoId(photoId) and
        withUserId(userId)
    }
  }

  open fun findManyFavouritedPhotos(userId: UserId, photoIdList: List<PhotoId>): List<FavouritedPhotoEntity> {
    return FavouritedPhotos.select {
      withUserId(userId) and
        withPhotoIdIn(photoIdList)
    }
      .limit(photoIdList.size)
      .map { resultRow -> FavouritedPhotoEntity.fromResultRow(resultRow) }
  }

  open fun isPhotoFavourited(photoId: PhotoId, userId: UserId): Boolean {
    return FavouritedPhotos.select {
      withPhotoId(photoId) and
        withUserId(userId)
    }
      .firstOrNull()
      ?.let { true } ?: false
  }

  open fun countFavouritesByPhotoId(photoId: PhotoId): Long {
    return FavouritedPhotos.select {
      withPhotoId(photoId)
    }
      .count()
      .toLong()
  }

  open fun countFavouritesByPhotoIdList(photoIdList: List<PhotoId>): Map<Long, Long> {
    val resultMap = hashMapOf<Long, Long>()

    FavouritedPhotos
      .slice(FavouritedPhotos.id.count(), FavouritedPhotos.photoId)
      .select {
        withPhotoIdIn(photoIdList)
      }
      .groupBy(FavouritedPhotos.photoId)
      .forEach { resultRow ->
        val id = resultRow[FavouritedPhotos.photoId]
        val count = resultRow[FavouritedPhotos.id.count()].toLong()

        resultMap[id] = count
      }

    return resultMap
  }

  open fun deleteAllFavouritesByPhotoId(photoId: PhotoId) {
    FavouritedPhotos.deleteWhere {
      withPhotoId(photoId)
    }
  }

  /**
   * For test purposes
   * */

  open fun testFindAll(): List<FavouritedPhotoEntity> {
    return FavouritedPhotos.selectAll()
      .map { resultRow -> FavouritedPhotoEntity.fromResultRow(resultRow) }
  }

  /**
   * FavouritedPhoto must have one of the ids from the list
   * */
  private fun SqlExpressionBuilder.withPhotoIdIn(photoIdList: List<PhotoId>): Op<Boolean> {
    return FavouritedPhotos.photoId.inList(photoIdList.map { it.id })
  }

  /**
   * FavouritedPhoto must have this photoId
   * */
  private fun SqlExpressionBuilder.withPhotoId(photoId: PhotoId): Op<Boolean> {
    return FavouritedPhotos.photoId.eq(photoId.id)
  }

  /**
   * FavouritedPhoto must have this userId
   * */
  private fun SqlExpressionBuilder.withUserId(userId: UserId): Op<Boolean> {
    return FavouritedPhotos.userId.eq(userId.id)
  }
}