package com.kirakishou.photoexchange.database.table

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object GalleryPhotos : Table() {
  val id = long(Field.ID).primaryKey().autoIncrement()
  val photoId = long(Field.PHOTO_ID).references(Photos.id, ReferenceOption.CASCADE).index(Index.PHOTO_ID_INDEX, true)
  val uploadedOn = datetime(Field.UPLOADED_ON).index(Index.UPLOADED_ON)

  object Field {
    const val ID = "id"
    const val PHOTO_ID = "photo_id"
    const val UPLOADED_ON = "uploaded_on"
  }

  object Index {
    const val PHOTO_ID_INDEX = "gallery_photos_photo_id_index"
    const val UPLOADED_ON = "gallery_photos_uploaded_on_index"
  }
}