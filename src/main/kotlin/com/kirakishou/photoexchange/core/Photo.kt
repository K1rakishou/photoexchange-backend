package com.kirakishou.photoexchange.core

import com.kirakishou.photoexchange.util.TimeUtils
import org.joda.time.DateTime

data class Photo(
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

  companion object {
    fun empty(): Photo {
      return Photo(
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
  }

}