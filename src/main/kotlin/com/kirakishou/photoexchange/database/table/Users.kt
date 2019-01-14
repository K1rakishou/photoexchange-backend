package com.kirakishou.photoexchange.database.table

import core.SharedConstants
import org.jetbrains.exposed.sql.Table

object Users : Table() {
  val id = long(Field.ID).primaryKey().autoIncrement()
  val userUuid = varchar(Field.USER_UUID, SharedConstants.FULL_USER_UUID_LEN).index(Index.USER_UUID, true)
  val firebaseToken = varchar(Field.FIREBASE_TOKEN, SharedConstants.MAX_FIREBASE_TOKEN_LEN).nullable()
  val lastLoginTime = datetime(Field.LAST_LOGIN_TIME).index(Index.LAST_LOGIN_TIME)

  object Field {
    const val ID = "id"
    const val USER_UUID = "user_uuid"
    const val FIREBASE_TOKEN = "firebase_token"
    const val LAST_LOGIN_TIME = "last_login_time"
  }

  object Index {
    const val USER_UUID = "users_user_uuid_index"
    const val LAST_LOGIN_TIME = "users_last_login_time_index"
  }
}