package com.kirakishou.photoexchange.database.pgsql.dao

import com.kirakishou.photoexchange.database.pgsql.entity.PhotoEntity
import com.kirakishou.photoexchange.database.pgsql.table.Photos
import org.jetbrains.exposed.sql.*

open class PhotosDao {

  open fun save(photoEntity: PhotoEntity): PhotoEntity {
    val id = Photos.insert {
      it[exchangedPhotoId] = photoEntity.exchangedPhotoId
      it[locationMapId] = photoEntity.locationMapId
      it[userId] = photoEntity.userId
      it[photoName] = photoEntity.photoName
      it[isPublic] = photoEntity.isPublic
      it[lon] = photoEntity.lon
      it[lat] = photoEntity.lat
      it[uploadedOn] = photoEntity.uploadedOn
      it[deletedOn] = photoEntity.deletedOn
      it[ipHash] = photoEntity.ipHash
    } get Photos.id

    return photoEntity.copy(photoId = id!!)
  }

  open fun findOldestEmptyPhoto(userId: String): PhotoEntity {
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

  open fun updatePhotoAsEmpty(photoId: Long): Boolean {
    return Photos.update({ withPhotoId(photoId) }) {
      it[exchangedPhotoId] = PhotoEntity.EMPTY_PHOTO_ID
    } == 1
  }

  open fun updatePhotoSetReceiverId(photoId: Long, receiverId: Long): Boolean {
    return Photos.update({ withPhotoId(photoId) }) {
      it[exchangedPhotoId] = receiverId
    } == 1
  }

  open fun findAllByUserId(userId: String): List<PhotoEntity> {
    return Photos.select {
      withUserId(userId)
    }
      .toList()
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findPhotosByNames(userId: String, photoNameList: List<String>): List<PhotoEntity> {
    return Photos.select {
      withUserId(userId) and
        withPhotoNameIn(photoNameList) and
        exchanged() and
        hasLocationMap()
    }
      .toList()
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findPhotosByName(photoNameList: List<String>): List<PhotoEntity> {
    return Photos.select {
      withPhotoNameIn(photoNameList) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .limit(photoNameList.size)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findManyByIds(photoIdList: List<Long>, sortAscending: Boolean = true): List<PhotoEntity> {
    return Photos.select {
      withPhotoIdIn(photoIdList) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, sortAscending)
      .limit(photoIdList.size)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findPageOfUploadedPhotos(userId: String, lastUploadedOn: Long, count: Int): List<PhotoEntity> {
    return Photos.select {
      withUserId(userId) and
        uploadedEarlierThan(lastUploadedOn) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .limit(count)
      .map { resultRow -> PhotoEntity.fromResultRow(resultRow) }
  }

  open fun findPageOfReceivedPhotos(userId: String, lastUploadedOn: Long, count: Int): List<PhotoEntity> {
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

  open fun findById(uploaderPhotoId: Long): PhotoEntity {
    return Photos.select {
      withPhotoId(uploaderPhotoId) and
        notDeleted()
    }
      .firstOrNull()
      ?.let { resultRow -> PhotoEntity.fromResultRow(resultRow) }
      ?: PhotoEntity.empty()
  }

  open fun findByPhotoName(photoName: String): PhotoEntity {
    return Photos.select {
      withPhotoName(photoName) and
        notDeleted()
    }
      .firstOrNull()
      ?.let { resultRow -> PhotoEntity.fromResultRow(resultRow) }
      ?: PhotoEntity.empty()
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

  open fun updateSetLocationMapId(photoId: Long, locationMapId: Long): Boolean {
    return Photos.update({ withPhotoId(photoId) }) {
      it[Photos.locationMapId] = locationMapId
    } == 1
  }

  open fun updateManyAsDeleted(currentTime: Long, photoIdList: List<Long>): Boolean {
    return Photos.update({ withPhotoIdIn(photoIdList) }) {
      it[Photos.deletedOn] = currentTime
    } == photoIdList.size
  }

  open fun deleteById(photoId: Long): Boolean {
    return Photos.deleteWhere {
      withPhotoId(photoId)
    } == 1
  }

  open fun deleteAll(photoIdList: List<Long>): Boolean {
    return Photos.deleteWhere {
      withPhotoIdIn(photoIdList)
    } == photoIdList.size
  }

  open fun photoNameExists(generatedName: String): Boolean {
    return Photos.select {
      withPhotoName(generatedName)
    }
      .firstOrNull()
      ?.let { true } ?: false
  }

  //TODO: tests for case when DB is empty should return 0 and not throw any exceptions
  open fun countFreshUploadedPhotosSince(userId: String, time: Long): Int {
    return Photos.select {
      withUserId(userId) and
        uploadedLaterThan(time) and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .count()
  }

  open fun countFreshExchangedPhotos(userId: String, time: Long): Int {
    return Photos.select {
      withUserId(userId) and
        uploadedLaterThan(time) and
        exchanged() and
        notDeleted()
    }
      .orderBy(Photos.uploadedOn, false)
      .count()
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
  private fun SqlExpressionBuilder.withPhotoId(photoId: Long): Op<Boolean> {
    return Photos.id.eq(photoId)
  }

  /**
   * Photo must have one of the ids from the list
   * */
  private fun SqlExpressionBuilder.withPhotoIdIn(photoIdList: List<Long>): Op<Boolean> {
    return Photos.id.inList(photoIdList)
  }

  /**
   * Photo must have this name
   * */
  private fun SqlExpressionBuilder.withPhotoName(photoName: String): Op<Boolean> {
    return Photos.photoName.eq(photoName)
  }

  /**
   * Photo must have one of the names from the list
   * */
  private fun SqlExpressionBuilder.withPhotoNameIn(photoNameList: List<String>): Op<Boolean> {
    return Photos.photoName.inList(photoNameList)
  }

  /**
   * Photo must belong to this user
   * */
  private fun SqlExpressionBuilder.withUserId(userId: String): Op<Boolean> {
    return Photos.userId.eq(userId)
  }

  /**
   * Photo must not belong to this user (so the user won't exchange with themselves)
   * */
  private fun SqlExpressionBuilder.withUserIdNot(userId: String): Op<Boolean> {
    return Photos.userId.neq(userId)
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
    return Photos.exchangedPhotoId.eq(PhotoEntity.EMPTY_PHOTO_ID)
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