package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.core.FirebaseToken
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.entity.UserEntity
import com.kirakishou.photoexchange.database.table.Users
import org.jetbrains.exposed.sql.*

open class UsersDao {

  open fun save(newUserUuid: UserUuid): UserEntity {
    val id = Users.insert {
      it[Users.userUuid] = newUserUuid.uuid
    } get Users.id

    return UserEntity.create(UserId(id!!), newUserUuid)
  }

  open fun userExists(userUuid: UserUuid): Boolean {
    return Users.select {
      withUserUuid(userUuid)
    }
      .firstOrNull()
      ?.let { true } ?: false
  }

  open fun getUser(userUuid: UserUuid): UserEntity {
    return Users.select {
      withUserUuid(userUuid)
    }
      .firstOrNull()
      ?.let { UserEntity.fromResultRow(it) } ?: UserEntity.empty()
  }

  open fun getUser(userId: UserId): UserEntity {
    return Users.select {
      withUserId(userId)
    }
      .firstOrNull()
      ?.let { UserEntity.fromResultRow(it) } ?: UserEntity.empty()
  }

  open fun updateFirebaseToken(userUuid: UserUuid, newToken: FirebaseToken): Boolean {
    return Users.update({ withUserUuid(userUuid) }) {
      it[Users.firebaseToken] = newToken.token
    } == 1
  }

  /**
   * User must have this userUuid
   * */
  private fun SqlExpressionBuilder.withUserUuid(userUuid: UserUuid): Op<Boolean> {
    return Users.userUuid.eq(userUuid.uuid)
  }

  /**
   * User must have this userId
   * */
  private fun SqlExpressionBuilder.withUserId(userId: UserId): Op<Boolean> {
    return Users.id.eq(userId.id)
  }
}