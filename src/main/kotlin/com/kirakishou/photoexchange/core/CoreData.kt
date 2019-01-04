package com.kirakishou.photoexchange.core

import core.SharedConstants

//TODO: make these classes inlined once that feature is supported by mockito
data class FirebaseToken(val token: String) {
  fun isDefault() = token == SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN
  fun isEmpty() = token.isEmpty()
  fun isNotEmpty() = !isEmpty()

  companion object {
    fun default(): FirebaseToken = FirebaseToken(SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN)
    fun empty(): FirebaseToken = FirebaseToken("")
  }
}

data class PhotoId(val id: Long) {
  fun isEmpty() = id == EMPTY_PHOTO_ID

  companion object {
    const val EMPTY_PHOTO_ID = -1L
    fun empty(): PhotoId = PhotoId(EMPTY_PHOTO_ID)
  }
}

data class UserId(val id: Long) {
  fun isEmpty() = id == EMPTY_USER_ID

  companion object {
    const val EMPTY_USER_ID = -1L
    fun empty(): UserId = UserId(EMPTY_USER_ID)
  }
}

data class ExchangedPhotoId(val id: Long) {
  fun isEmpty() = id == EMPTY_EXCHANGED_PHOTO_ID

  fun toPhotoId(): PhotoId = PhotoId(id)

  companion object {
    const val EMPTY_EXCHANGED_PHOTO_ID = -1L
    fun empty(): ExchangedPhotoId = ExchangedPhotoId(EMPTY_EXCHANGED_PHOTO_ID)
  }
}

data class LocationMapId(val id: Long) {
  fun isEmpty() = id == EMPTY_LOCATION_MAP_ID

  companion object {
    const val EMPTY_LOCATION_MAP_ID = -1L
    fun empty(): LocationMapId = LocationMapId(EMPTY_LOCATION_MAP_ID)
  }
}

data class PhotoName(val name: String) {
  fun isEmpty() = name.isEmpty()

  override fun toString() = name

  companion object {
    fun empty(): PhotoName = PhotoName("")
  }
}

data class IpHash(val hash: String) {
  fun isEmpty() = hash.isEmpty()

  override fun toString() = hash

  companion object {
    fun empty(): IpHash = IpHash("")
  }
}

data class UserUuid(val uuid: String) {
  fun isEmpty() = uuid.isEmpty()

  override fun toString() = uuid

  companion object {
    fun empty(): UserUuid = UserUuid("")
  }
}

data class FavouritedPhotoId(val id: Long) {
  fun isEmpty() = id == -1L

  companion object {
    fun empty(): FavouritedPhotoId = FavouritedPhotoId(-1L)
  }
}

data class ReportedPhotoId(val id: Long) {
  fun isEmpty() = id == -1L

  companion object {
    fun empty():  ReportedPhotoId = ReportedPhotoId(-1L)
  }
}

data class GalleryPhotoId(val id: Long) {
  fun isEmpty() = id == -1L

  companion object {
    fun empty():  GalleryPhotoId = GalleryPhotoId(-1L)
  }
}

data class BanId(val id: Long) {
  fun isEmpty() = id == -1L

  companion object {
    fun empty():  BanId = BanId(-1L)
  }
}