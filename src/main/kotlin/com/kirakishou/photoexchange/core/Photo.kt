package com.kirakishou.photoexchange.core

data class Photo(
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

  companion object {
    const val EMPTY_PHOTO_ID = -2L
    const val EMPTY_LOCATION_MAP_ID = -1L

    fun empty(): Photo {
      return Photo(
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
  }

}