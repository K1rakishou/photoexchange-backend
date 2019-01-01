package com.kirakishou.photoexchange.database.pgsql.dao

import com.kirakishou.photoexchange.core.FirebaseToken
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.pgsql.entity.UserEntity
import com.kirakishou.photoexchange.database.pgsql.table.Users
import org.jetbrains.exposed.sql.*

open class UsersDao {

  open fun save(newUserUuid: UserUuid): UserEntity {
    val id = Users.insert {
      it[userUuid] = newUserUuid.uuid
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
      ?.let { UserEntity.fromResultRow(it) } ?: UserEntity.empty(userUuid)
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

}