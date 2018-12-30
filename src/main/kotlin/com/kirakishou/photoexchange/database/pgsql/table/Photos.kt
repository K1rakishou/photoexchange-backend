package com.kirakishou.photoexchange.database.pgsql.table

import com.kirakishou.photoexchange.config.ServerSettings
import core.SharedConstants
import org.jetbrains.exposed.sql.Table

object Photos : Table() {
  val id = long(Field.PHOTO_ID).primaryKey().autoIncrement()
  val exchangedPhotoId = long(Field.EXCHANGED_PHOTO_ID).index(Index.EXCHANGED_PHOTO_ID)
  val locationMapId = long(Field.LOCATION_MAP_ID)
  val userId = varchar(Field.USER_ID, SharedConstants.MAX_USER_ID_LEN).index(Index.USER_ID)
  val photoName = varchar(Field.PHOTO_NAME, SharedConstants.MAX_PHOTO_NAME_LEN).index(Index.PHOTO_NAME)
  val isPublic = bool(Field.IS_PUBLIC)
  val lon = double(Field.LONGITUDE)
  val lat = double(Field.LATITUDE)
  val uploadedOn = long(Field.UPLOADED_ON).index(Index.UPLOADED_ON)
  val deletedOn = long(Field.DELETED_ON).index(Index.DELETED_ON)
  val ipHash = varchar(Field.IP_HASH, ServerSettings.IP_HASH_LENGTH).index(Index.IP_HASH)

  object Field {
    const val PHOTO_ID = "_id"
    const val EXCHANGED_PHOTO_ID = "exchanged_photo_id"
    const val LOCATION_MAP_ID = "location_map_id"
    const val USER_ID = "user_id"
    const val PHOTO_NAME = "photo_name"
    const val IS_PUBLIC = "is_public"
    const val LONGITUDE = "longitude"
    const val LATITUDE = "latitude"
    const val UPLOADED_ON = "uploaded_on"
    const val DELETED_ON = "deleted_on"
    const val IP_HASH = "ip_hash"
  }

  object Index {
    const val EXCHANGED_PHOTO_ID = "exchanged_photo_id_index"
    const val USER_ID = "user_id_index"
    const val PHOTO_NAME = "photo_name_index"
    const val UPLOADED_ON = "uploaded_on_index"
    const val DELETED_ON = "deleted_on_index"
    const val IP_HASH = "ip_hash_index"
  }
}