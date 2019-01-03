package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import com.kirakishou.photoexchange.database.table.Photos
import org.jetbrains.exposed.sql.*

open class PhotosDao {

  open fun save(photoEntity: PhotoEntity): PhotoEntity {
    val id = Photos.insert {
      it[Photos.userId] = photoEntity.userId.id
      it[Photos.exchangedPhotoId] = photoEntity.exchangedPhotoId.id
      it[Photos.locationMapId] = photoEntity.locationMapId.id
      it[Photos.photoName] = photoEntity.photoName.name
      it[Photos.isPublic] = photoEntity.isPublic
      it[Photos.lon] = photoEntity.lon
      it[Photos.lat] = photoEntity.lat
      it[Photos.uploadedOn] = photoEntity.uploadedOn
      it[Photos.deletedOn] = photoEntity.deletedOn
      it[Photos.ipHash] = photoEntity.ipHash.hash
    } get Photos.id

    return photoEntity.copy(photoId = PhotoId(id!!))
  }

  open fun findOldestEmptyPhoto(userId: UserId): PhotoEntity {
    return Photos.select {
      withUserIdNot(userId) and
        notExchanged() and
        hasLocationMap() and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, true)
      .limit(1)
      .firstOrNull()
      ?.let { resultRow -> PhotoEntity.fromResultRow(resultRow) }
      ?: PhotoEntity.empty()
  }

  open fun updatePhotoAsEmpty(photoId: PhotoId): Boolean {
    return Photos.update({ withPhotoId(photoId) }) {
      it[Photos.exchangedPhotoId] = ExchangedPhotoId.empty().id
    } == 1
  }

  open fun updatePhotoSetReceiverId(photoId: PhotoId, receiverId: ExchangedPhotoId): Boolean {
    return Photos.update({ withPhotoId(photoId) }) {
      it[Photos.exchangedPhotoId] = receiverId.id
    } == 1
  }

  open fun findAllByUserId(userId: UserId): List<PhotoEntity> {
    return Photos.select {
      withUserId(userId)
    }
      .toList()
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findPhotosByNames(userId: UserId, photoNameList: List<PhotoName>): List<PhotoEntity> {
    return Photos.select {
      withUserId(userId) and
        withPhotoNameIn(photoNameList) and
        exchanged() and
        hasLocationMap()
    }
      .toList()
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findPhotosByNames(photoNameList: List<PhotoName>): List<PhotoEntity> {
    return Photos.select {
      withPhotoNameIn(photoNameList) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .limit(photoNameList.size)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findManyByPhotoIdList(photoIdList: List<PhotoId>, sortAscending: Boolean): List<PhotoEntity> {
    return Photos.select {
      withPhotoIdIn(photoIdList) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, sortAscending)
      .limit(photoIdList.size)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findManyByExchangedIdList(exchangedIdList: List<ExchangedPhotoId>, sortAscending: Boolean): List<PhotoEntity> {
    return Photos.select {
      withExchangedPhotoIdIn(exchangedIdList) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, sortAscending)
      .limit(exchangedIdList.size)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findPageOfUploadedPhotos(userId: UserId, lastUploadedOn: Long, count: Int): List<PhotoEntity> {
    return Photos.select {
      withUserId(userId) and
        uploadedEarlierThan(lastUploadedOn) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .limit(count)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findPageOfReceivedPhotos(userId: UserId, lastUploadedOn: Long, count: Int): List<PhotoEntity> {
    return Photos.select {
      withUserId(userId) and
        exchanged() and
        uploadedEarlierThan(lastUploadedOn) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .limit(count)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findById(uploaderPhotoId: PhotoId): PhotoEntity {
    return Photos.select {
      withPhotoId(uploaderPhotoId) and
        notDeleted()
    }
      .firstOrNull()
      ?.let { resultRow -> PhotoEntity.fromResultRow(resultRow) }
      ?: PhotoEntity.empty()
  }

  open fun findByPhotoName(photoName: PhotoName): PhotoEntity {
    return Photos.select {
      withPhotoName(photoName) and
        notDeleted()
    }
      .firstOrNull()
      ?.let { resultRow -> PhotoEntity.fromResultRow(resultRow) }
      ?: PhotoEntity.empty()
  }

  open fun findManyByPhotoNameList(photoNameList: List<PhotoName>): List<PhotoEntity> {
    return Photos.select {
      withPhotoNameIn(photoNameList)
    }
      .limit(photoNameList.size)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findOldPhotos(earlierThanTime: Long, count: Int): List<PhotoEntity> {
    return Photos.select {
      uploadedEarlierThan(earlierThanTime) and
        exchanged() and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .limit(count)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findDeletedPhotos(deletedEarlierThanTime: Long, count: Int): List<PhotoEntity> {
    return Photos.select {
      deletedEarlierThan(deletedEarlierThanTime) and
        exchanged()
    }
      .orderBy(Photos.uploadedOn, false)
      .limit(count)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun updateSetLocationMapId(photoId: PhotoId, locationMapId: LocationMapId): Boolean {
    return Photos.update({ withPhotoId(photoId) }) {
      it[Photos.locationMapId] = locationMapId.id
    } == 1
  }

  open fun updateManyAsDeleted(currentTime: Long, photoIdList: List<PhotoId>): Boolean {
    return Photos.update({ withPhotoIdIn(photoIdList) }) {
      it[Photos.deletedOn] = currentTime
    } == photoIdList.size
  }

  open fun photoNameExists(generatedName: PhotoName): Boolean {
    return Photos.select {
      withPhotoName(generatedName)
    }
      .firstOrNull()
      ?.let { true } ?: false
  }

  //TODO: tests for case when DB is empty should return 0 and not throw any exceptions
  open fun countFreshUploadedPhotosSince(userId: UserId, time: Long): Int {
    return Photos.select {
      withUserId(userId) and
        uploadedLaterThan(time) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .count()
  }

  open fun countFreshExchangedPhotos(userId: UserId, time: Long): Int {
    return Photos.select {
      withUserId(userId) and
        uploadedLaterThan(time) and
        exchanged() and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .count()
  }

  open fun deleteById(photoId: PhotoId) {
    Photos.deleteWhere {
      withPhotoId(photoId)
    }
  }

  open fun deleteAll(photoIdList: List<PhotoId>) {
    Photos.deleteWhere {
      withPhotoIdIn(photoIdList)
    }
  }

  /**
   * For test purposes
   * */

  open fun testFindAll(): List<PhotoEntity> {
    return Photos.selectAll()
      .toList()
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  /**
   * Photo must be uploaded later than "time"
   * */
  private fun SqlExpressionBuilder.uploadedLaterThan(time: Long): Op<Boolean> {
    return Photos.uploadedOn.greater(time)
  }

  /**
   * Photo must be deleted (deleteOn > 0) and it must be deleted earlier than "time"
   * */
  private fun SqlExpressionBuilder.deletedEarlierThan(time: Long): Op<Boolean> {
    return Photos.deletedOn.greater(0L) and Photos.deletedOn.less(time)
  }

  /**
   * Photo must have this id
   * */
  private fun SqlExpressionBuilder.withPhotoId(photoId: PhotoId): Op<Boolean> {
    return Photos.id.eq(photoId.id)
  }

  /**
   * Photo must have one of the ids from the list
   * */
  private fun SqlExpressionBuilder.withPhotoIdIn(photoIdList: List<PhotoId>): Op<Boolean> {
    return Photos.id.inList(photoIdList.map { it.id })
  }

  private fun SqlExpressionBuilder.withExchangedPhotoIdIn(photoIdList: List<ExchangedPhotoId>): Op<Boolean> {
    return Photos.id.inList(photoIdList.map { it.id })
  }

  /**
   * Photo must have this name
   * */
  private fun SqlExpressionBuilder.withPhotoName(photoName: PhotoName): Op<Boolean> {
    return Photos.photoName.eq(photoName.name)
  }

  /**
   * Photo must have one of the names from the list
   * */
  private fun SqlExpressionBuilder.withPhotoNameIn(photoNameList: List<PhotoName>): Op<Boolean> {
    return Photos.photoName.inList(photoNameList.map { it.name })
  }

  /**
   * Photo must belong to this user
   * */
  private fun SqlExpressionBuilder.withUserId(userId: UserId): Op<Boolean> {
    return Photos.userId.eq(userId.id)
  }

  /**
   * Photo must not belong to this user (so the user won't exchange with themselves)
   * */
  private fun SqlExpressionBuilder.withUserIdNot(userId: UserId): Op<Boolean> {
    return Photos.userId.neq(userId.id)
  }

  /**
   * Photo must be uploaded earlier than "lastUploadedOn"
   * */
  private fun SqlExpressionBuilder.uploadedEarlierThan(lastUploadedOn: Long): Op<Boolean> {
    return Photos.uploadedOn.less(lastUploadedOn)
  }

  /**
   * Photo must not be exchanged photo
   * */
  private fun SqlExpressionBuilder.notExchanged(): Op<Boolean> {
    return Photos.exchangedPhotoId.eq(ExchangedPhotoId.empty().id)
  }

  /**
   * Photo must be exchanged
   * */
  private fun SqlExpressionBuilder.exchanged(): Op<Boolean> {
    return Photos.exchangedPhotoId.greater(0L)
  }

  /**
   * Photo must have location map
   * */
  private fun SqlExpressionBuilder.hasLocationMap(): Op<Boolean> {
    return Photos.locationMapId.greater(0L)
  }

  /**
   * Photo must not be deleted
   * */
  private fun SqlExpressionBuilder.notDeleted(): Op<Boolean> {
    return Photos.deletedOn.eq(0L)
  }
}