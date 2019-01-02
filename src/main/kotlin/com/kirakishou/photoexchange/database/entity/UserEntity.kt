package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.core.FirebaseToken
import com.kirakishou.photoexchange.core.User
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.table.Users
import core.SharedConstants
import org.jetbrains.exposed.sql.ResultRow

data class UserEntity(
  val userId: UserId,
  val userUuid: UserUuid,
  val firebaseToken: FirebaseToken
) {

  fun isEmpty() = userId.isEmpty()

  fun toUser(): User {
    return User(userId, userUuid, firebaseToken)
  }

  companion object {
    fun create(
      id: UserId,
      userUuid: UserUuid,
      token: FirebaseToken = FirebaseToken(SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN)
    ): UserEntity {
      return UserEntity(id, userUuid, token)
    }

    fun empty(userUuid: UserUuid): UserEntity {
      return UserEntity(
        UserId.empty(),
        userUuid,
        FirebaseToken(SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN)
      )
    }

    fun fromResultRow(resultRow: ResultRow): UserEntity {
      return UserEntity(
        UserId(resultRow[Users.id]),
        UserUuid(resultRow[Users.userUuid]),
        FirebaseToken(resultRow[Users.firebaseToken])
      )
    }
  }

}