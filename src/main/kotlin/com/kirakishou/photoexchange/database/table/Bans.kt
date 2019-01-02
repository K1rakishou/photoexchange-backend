package com.kirakishou.photoexchange.database.table

import com.kirakishou.photoexchange.config.ServerSettings
import org.jetbrains.exposed.sql.Table

object Bans : Table() {
  val id = long(Field.ID).primaryKey().autoIncrement()
  val userId = long(Field.USER_ID).references(Users.id).index(Index.USER_ID, true)
  val ipHash = varchar(Field.IP_HASH, ServerSettings.IP_HASH_LENGTH).index(Index.IP_HASH, true)
  val bannedOn = long(Field.BANNED_ON)

  object Field {
    const val ID = "id"
    const val USER_ID = "user_id"
    const val IP_HASH = "ip_hash"
    const val BANNED_ON = "banned_on"
  }

  object Index {
    const val USER_ID = "bans_user_id_index"
    const val IP_HASH = "bans_ip_hash_index"
  }
}