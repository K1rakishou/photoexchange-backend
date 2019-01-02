package com.kirakishou.photoexchange.database.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object ReportedPhotos : Table() {
  val id = long(Field.ID).primaryKey().autoIncrement()
  val photoId = long(Field.PHOTO_ID).references(Photos.id, ReferenceOption.CASCADE).index(Index.PHOTO_ID_INDEX)
  val userId = long(Field.USER_ID).references(Users.id, ReferenceOption.CASCADE).index(Index.USER_ID_INDEX)

  object Field {
    const val ID = "_id"
    const val PHOTO_ID = "photo_id"
    const val USER_ID = "user_id"
  }

  object Index {
    const val PHOTO_ID_INDEX = "photo_id_index"
    const val USER_ID_INDEX = "user_id_index"
  }
}