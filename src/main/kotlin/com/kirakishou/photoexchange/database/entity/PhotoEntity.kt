package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.table.Photos
import org.jetbrains.exposed.sql.ResultRow

data class PhotoEntity(
  val photoId: PhotoId,
  val userId: UserId,
  val exchangedPhotoId: ExchangedPhotoId,
  val locationMapId: LocationMapId,
  val photoName: PhotoName,
  val isPublic: Boolean,
  val lon: Double,
  val lat: Double,
  val uploadedOn: Long,
  val deletedOn: Long,
  val ipHash: IpHash
) {
  fun isEmpty() = photoId.isEmpty()

  fun isAnonymous(): Boolean {
    return lon == -1.0 && lat == -1.0
  }

  fun toPhoto(): Photo {
    return Photo(
      photoId,
      userId,
      exchangedPhotoId,
      locationMapId,
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

    fun create(
      userId: UserId,
      photoName: PhotoName,
      isPublic: Boolean,
      lon: Double,
      lat: Double,
      uploadedOn: Long,
      ipHash: IpHash
    ): PhotoEntity {
      return PhotoEntity(
        PhotoId.empty(),
        userId,
        ExchangedPhotoId.photoIsExchanging(),
        LocationMapId.empty(),
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
        PhotoId.empty(),
        UserId.empty(),
        ExchangedPhotoId.empty(),
        LocationMapId.empty(),
        PhotoName.empty(),
        false,
        0.0,
        0.0,
        0L,
        0L,
        IpHash.empty()
      )
    }

    fun fromResultRow(resultRow: ResultRow): PhotoEntity {
      return PhotoEntity(
        PhotoId(resultRow[Photos.id]),
        UserId(resultRow[Photos.userId]),
        ExchangedPhotoId(resultRow[Photos.exchangedPhotoId]),
        LocationMapId(resultRow[Photos.locationMapId]),
        PhotoName(resultRow[Photos.photoName]),
        resultRow[Photos.isPublic],
        resultRow[Photos.lon],
        resultRow[Photos.lat],
        resultRow[Photos.uploadedOn],
        resultRow[Photos.deletedOn],
        IpHash(resultRow[Photos.ipHash])
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