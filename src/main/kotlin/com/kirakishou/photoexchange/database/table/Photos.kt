package com.kirakishou.photoexchange.database.table

import com.kirakishou.photoexchange.config.ServerSettings
import core.SharedConstants
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Photos : Table() {
  val id = long(Field.ID).primaryKey().autoIncrement()
  val exchangeState = integer(Field.EXCHANGED_STATE).index(Index.EXCHANGE_STATE)
  val userId = long(Field.USER_ID).references(Users.id, ReferenceOption.CASCADE).index(Index.USER_ID)
  val exchangedPhotoId = long(Field.EXCHANGED_PHOTO_ID).index(Index.EXCHANGED_PHOTO_ID)
  val locationMapId = long(Field.LOCATION_MAP_ID)
  val photoName = varchar(Field.PHOTO_NAME, SharedConstants.MAX_PHOTO_NAME_LEN).index(Index.PHOTO_NAME, true)
  val isPublic = bool(Field.IS_PUBLIC)
  val lon = double(Field.LONGITUDE)
  val lat = double(Field.LATITUDE)
  val uploadedOn = datetime(Field.UPLOADED_ON).index(Index.UPLOADED_ON)
  val deletedOn = datetime(Field.DELETED_ON).index(Index.DELETED_ON)
  val ipHash = varchar(Field.IP_HASH, ServerSettings.IP_HASH_LENGTH).index(Index.IP_HASH)

  object Field {
    const val ID = "id"
    const val EXCHANGED_STATE = "exchange_state"
    const val USER_ID = "user_id"
    const val EXCHANGED_PHOTO_ID = "exchanged_photo_id"
    const val LOCATION_MAP_ID = "location_map_id"
    const val PHOTO_NAME = "photo_name"
    const val IS_PUBLIC = "is_public"
    const val LONGITUDE = "longitude"
    const val LATITUDE = "latitude"
    const val UPLOADED_ON = "uploaded_on"
    const val DELETED_ON = "deleted_on"
    const val IP_HASH = "ip_hash"
  }

  object Index {
    const val USER_ID = "photos_user_id_index"
    const val EXCHANGE_STATE = "photos_exchange_state_index"
    const val EXCHANGED_PHOTO_ID = "photos_exchanged_photo_id_index"
    const val PHOTO_NAME = "photos_photo_name_index"
    const val UPLOADED_ON = "photos_uploaded_on_index"
    const val DELETED_ON = "photos_deleted_on_index"
    const val IP_HASH = "photos_ip_hash_index"
  }
}