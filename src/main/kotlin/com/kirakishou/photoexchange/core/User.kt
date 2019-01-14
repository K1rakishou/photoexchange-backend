package com.kirakishou.photoexchange.core


data class User(
  val id: UserId,
  val userUuid: UserUuid,
  val firebaseToken: FirebaseToken,
  val lastLoginTime: Long
) {

  fun isEmpty() = id.isEmpty()

  companion object {
    fun empty(): User {
      return User(
        UserId.empty(),
        UserUuid.empty(),
        FirebaseToken.default(),
        -1L
      )
    }
  }

}