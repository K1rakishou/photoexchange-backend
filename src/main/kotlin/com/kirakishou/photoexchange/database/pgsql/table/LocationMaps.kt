package com.kirakishou.photoexchange.database.pgsql.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object LocationMaps : Table() {
  val id = long(Field.ID).primaryKey().autoIncrement()
  val photoId = long(Field.PHOTO_ID).references(Photos.id, ReferenceOption.CASCADE).index(Index.PHOTO_ID)
  val attemptsCount = integer(Field.ATTEMPTS_COUNT).default(0)
  val mapStatus = integer(Field.MAP_STATUS)
  val nextAttemptTime = long(Field.NEXT_ATTEMPT_TIME)

  object Field {
    const val ID = "id"
    const val PHOTO_ID = "photo_id"
    const val ATTEMPTS_COUNT = "attemps_count"
    const val MAP_STATUS = "map_status"
    const val NEXT_ATTEMPT_TIME = "next_attempt_time"
  }

  object Index {
    const val PHOTO_ID = "photo_id_index"
  }
}