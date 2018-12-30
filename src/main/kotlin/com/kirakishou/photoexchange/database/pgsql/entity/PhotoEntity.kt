package com.kirakishou.photoexchange.database.pgsql.entity

import com.kirakishou.photoexchange.core.Photo
import com.kirakishou.photoexchange.database.pgsql.table.Photos
import org.jetbrains.exposed.sql.ResultRow

data class PhotoEntity(
  val photoId: Long,
  val exchangedPhotoId: Long,
  val locationMapId: Long,
  val userId: String,
  val photoName: String,
  val isPublic: Boolean,
  val lon: Double,
  val lat: Double,
  val uploadedOn: Long,
  val deletedOn: Long,
  val ipHash: String
) {
  fun isEmpty(): Boolean {
    return photoId == EMPTY_PHOTO_ID
  }

  fun isAnonymous(): Boolean {
    return lon == -1.0 && lat == -1.0
  }

  fun toPhoto(): Photo {
    return Photo(
      photoId,
      exchangedPhotoId,
      locationMapId,
      userId,
      photoName,
      isPublic,
      lon,
      lat,
      uploadedOn,
      deletedOn,
      ipHash
    )
  }

  companion object {
    /**
    This is a default exchangedPhotoId when photo is uploading.
    This is used so other uploading requests won't be able to find this photo to do the exchange
    Upon successful exchange exchangedPhotoId is set to theirPhoto.photoId
    Upon unsuccessful exchange (when there are no photos to exchange with) exchangedPhotoId is set to EMPTY_PHOTO_ID
     */
    const val PHOTO_IS_EXCHANGING = -1L

    const val EMPTY_PHOTO_ID = -2L
    const val EMPTY_LOCATION_MAP_ID = -1L

    fun create(
      userId: String,
      photoName: String,
      isPublic: Boolean,
      lon: Double,
      lat: Double,
      uploadedOn: Long,
      ipHash: String
    ): PhotoEntity {
      return PhotoEntity(
        EMPTY_PHOTO_ID,
        PHOTO_IS_EXCHANGING,
        EMPTY_LOCATION_MAP_ID,
        userId,
        photoName,
        isPublic,
        lon,
        lat,
        uploadedOn,
        0L,
        ipHash
      )
    }

    fun empty(): PhotoEntity {
      return PhotoEntity(
        EMPTY_PHOTO_ID,
        EMPTY_PHOTO_ID,
        EMPTY_LOCATION_MAP_ID,
        "",
        "",
        false,
        0.0,
        0.0,
        0L,
        0L,
        ""
      )
    }

    fun fromResultRow(resultRow: ResultRow): PhotoEntity {
      return PhotoEntity(
        resultRow[Photos.id],
        resultRow[Photos.exchangedPhotoId],
        resultRow[Photos.locationMapId],
        resultRow[Photos.userId],
        resultRow[Photos.photoName],
        resultRow[Photos.isPublic],
        resultRow[Photos.lon],
        resultRow[Photos.lat],
        resultRow[Photos.uploadedOn],
        resultRow[Photos.deletedOn],
        resultRow[Photos.ipHash]
      )
    }
  }

  override fun equals(other: Any?): Boolean {
    if (other == null) {
      return false
    }

    if (other === this) {
      return true
    }

    if (other::class.java != this::class.java) {
      return false
    }

    other as PhotoEntity

    return other.photoId == photoId && other.photoName == photoName
  }

  override fun hashCode(): Int {
    return photoId.hashCode() * photoName.hashCode()
  }
}