package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.core.FirebaseToken
import com.kirakishou.photoexchange.core.User
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.table.Users
import com.kirakishou.photoexchange.util.TimeUtils
import core.SharedConstants
import org.jetbrains.exposed.sql.ResultRow

data class UserEntity(
  val userId: UserId,
  val userUuid: UserUuid,
  val firebaseToken: FirebaseToken,
  val lastLoginTime: Long
) {

  fun isEmpty() = userId.isEmpty()

  fun toUser(): User {
    return User(userId, userUuid, firebaseToken, lastLoginTime)
  }

  companion object {
    fun create(
      id: UserId,
      userUuid: UserUuid,
      token: FirebaseToken = FirebaseToken(SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN)
    ): UserEntity {
      return UserEntity(id, userUuid, token, TimeUtils.getCurrentDateTime().millis)
    }

    fun empty(): UserEntity {
      return UserEntity(
        UserId.empty(),
        UserUuid.empty(),
        FirebaseToken(SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN),
        -1L
      )
    }

    fun fromResultRow(resultRow: ResultRow): UserEntity {
      return UserEntity(
        UserId(resultRow[Users.id]),
        UserUuid(resultRow[Users.userUuid]),
        FirebaseToken(resultRow[Users.firebaseToken] ?: FirebaseToken.default().token),
        resultRow[Users.lastLoginTime].millis
      )
    }
  }

}