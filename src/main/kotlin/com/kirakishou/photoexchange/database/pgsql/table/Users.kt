package com.kirakishou.photoexchange.database.pgsql.table

import core.SharedConstants
import org.jetbrains.exposed.sql.Table

object Users : Table() {
  val id = long(Field.ID).primaryKey().autoIncrement()
  val userUuid = varchar(Field.USER_UUID_STRING, SharedConstants.MAX_USER_ID_LEN).index(Index.USER_UUID_STRING)
  val firebaseToken = varchar(Field.FIREBASE_TOKEN, SharedConstants.MAX_FIREBASE_TOKEN_LEN)

  object Field {
    const val ID = "id"
    const val USER_UUID_STRING = "user_uuid_string"
    const val FIREBASE_TOKEN = "firebase_token"
  }

  object Index {
    const val USER_UUID_STRING = "user_uuid_string_index"
  }
}