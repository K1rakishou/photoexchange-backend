package com.kirakishou.photoexchange.database.pgsql.dao

import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.database.pgsql.entity.FavouritedPhotoEntity
import com.kirakishou.photoexchange.database.pgsql.table.FavouritedPhotos
import org.jetbrains.exposed.sql.*

open class FavouritedPhotosDao {

  //TODO: test when there is already favourited photo with the same photoId and userId
  open fun favouritePhoto(photoId: PhotoId, userId: UserId): Boolean {
    val id = FavouritedPhotos.insert {
      it[this.photoId] = photoId.id
      it[this.userId] = userId.id
    } get FavouritedPhotos.id

    return id != null
  }

  //TODO: test when there is no photo with provided photoId and userId
  open fun unfavouritePhoto(photoId: PhotoId, userId: UserId): Boolean {
    return FavouritedPhotos.deleteWhere {
      withPhotoId(photoId) and
        withUserId(userId)
    } == 1
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

  //TODO: NOT TESTED AT ALL
  //TODO: test ordering of the returned list
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

  private fun SqlExpressionBuilder.withPhotoIdIn(photoIdList: List<PhotoId>): Op<Boolean> {
    return FavouritedPhotos.photoId.inList(photoIdList.map { it.id })
  }

  private fun SqlExpressionBuilder.withPhotoId(photoId: PhotoId): Op<Boolean> {
    return FavouritedPhotos.photoId.eq(photoId.id)
  }

  private fun SqlExpressionBuilder.withUserId(userId: UserId): Op<Boolean> {
    return FavouritedPhotos.userId.eq(userId.id)
  }
}