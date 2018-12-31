package com.kirakishou.photoexchange.core

data class Photo(
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

  companion object {
    fun empty(): Photo {
      return Photo(
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
  }

}