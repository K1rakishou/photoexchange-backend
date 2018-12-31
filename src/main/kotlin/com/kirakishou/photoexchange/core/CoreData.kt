package com.kirakishou.photoexchange.core

import core.SharedConstants

inline class FirebaseToken(val token: String) {
  fun isDefault() = token == SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN
  fun isEmpty() = token.isEmpty()

  companion object {
    fun default(): FirebaseToken = FirebaseToken(SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN)
    fun empty(): FirebaseToken = FirebaseToken("")
  }
}

inline class PhotoId(val id: Long) {
  fun isEmpty() = id == -2L

  companion object {
    fun empty(): PhotoId = PhotoId(-2L)
  }
}

inline class UserId(val id: Long) {
  fun isEmpty() = id == -1L

  companion object {
    fun empty(): UserId = UserId(-1L)
  }
}

inline class ExchangedPhotoId(val id: Long) {
  /**
    This is a default exchangedPhotoId when photo is uploading.
    This is used so other uploading requests won't be able to find this photo to do the exchange
    Upon successful exchange exchangedPhotoId is set to theirPhoto.photoId
    Upon unsuccessful exchange (when there are no photos to exchange with) exchangedPhotoId is set to EMPTY_PHOTO_ID
   */
  fun isExchanging() = id == -1L

  fun empty() = id == -2L

  companion object {
    fun photoIsExchanging(): ExchangedPhotoId = ExchangedPhotoId(-1L)
    fun empty(): ExchangedPhotoId = ExchangedPhotoId(-2L)
  }
}

inline class LocationMapId(val id: Long) {
  fun isEmpty() = id == -1L

  companion object {
    fun empty(): LocationMapId = LocationMapId(-1L)
  }
}

inline class PhotoName(val name: String) {
  fun isEmpty() = name.isEmpty()

  companion object {
    fun empty(): PhotoName = PhotoName("")
  }
}

inline class IpHash(val hash: String) {
  fun isEmpty() = hash.isEmpty()

  companion object {
    fun empty(): IpHash = IpHash("")
  }
}

inline class UserUuid(val uuid: String) {
  fun isEmpty() = uuid.isEmpty()

  companion object {
    fun empty(): UserUuid = UserUuid("")
  }
}
