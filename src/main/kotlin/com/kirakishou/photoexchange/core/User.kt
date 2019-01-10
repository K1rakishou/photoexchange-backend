package com.kirakishou.photoexchange.core

import com.kirakishou.photoexchange.util.TimeUtils
import org.joda.time.DateTime

data class User(
  val id: UserId,
  val userUuid: UserUuid,
  val firebaseToken: FirebaseToken,
  val lastLoginTime: DateTime
) {

  fun isEmpty() = id.isEmpty()

  companion object {
    fun empty(): User {
      return User(
        UserId.empty(),
        UserUuid.empty(),
        FirebaseToken.default(),
        TimeUtils.getCurrentDateTime()
      )
    }
  }

}