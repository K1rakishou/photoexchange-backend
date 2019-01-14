package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.table.Photos
import com.kirakishou.photoexchange.util.TimeUtils
import org.jetbrains.exposed.sql.ResultRow
import org.joda.time.DateTime

data class PhotoEntity(
  val photoId: PhotoId,
  val exchangeState: ExchangeState,
  val userId: UserId,
  val exchangedPhotoId: ExchangedPhotoId,
  val locationMapId: LocationMapId,
  val photoName: PhotoName,
  val isPublic: Boolean,
  val lon: Double,
  val lat: Double,
  val uploadedOn: DateTime,
  val deletedOn: DateTime,
  val ipHash: IpHash
) {
  fun isEmpty() = photoId.isEmpty()

  fun isAnonymous(): Boolean {
    return lon == -1.0 && lat == -1.0
  }

  fun toPhoto(): Photo {
    return Photo(
      photoId,
      exchangeState,
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
      uploadedOn: DateTime,
      ipHash: IpHash
    ): PhotoEntity {
      return PhotoEntity(
        PhotoId.empty(),
        ExchangeState.ReadyToExchange,
        userId,
        ExchangedPhotoId.empty(),
        LocationMapId.empty(),
        photoName,
        isPublic,
        lon,
        lat,
        uploadedOn,
        TimeUtils.dateTimeZero,
        ipHash
      )
    }

    fun empty(): PhotoEntity {
      return PhotoEntity(
        PhotoId.empty(),
        ExchangeState.ReadyToExchange,
        UserId.empty(),
        ExchangedPhotoId.empty(),
        LocationMapId.empty(),
        PhotoName.empty(),
        false,
        0.0,
        0.0,
        TimeUtils.dateTimeZero,
        TimeUtils.dateTimeZero,
        IpHash.empty()
      )
    }

    fun fromResultRow(resultRow: ResultRow): PhotoEntity {
      return PhotoEntity(
        PhotoId(resultRow[Photos.id]),
        ExchangeState.fromInt(resultRow[Photos.exchangeState]),
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

    fun fromPhoto(photo: Photo): PhotoEntity {
      return PhotoEntity(
        photo.photoId,
        photo.exchangeState,
        photo.userId,
        photo.exchangedPhotoId,
        photo.locationMapId,
        photo.photoName,
        photo.isPublic,
        photo.lon,
        photo.lat,
        photo.uploadedOn,
        photo.deletedOn,
        photo.ipHash
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