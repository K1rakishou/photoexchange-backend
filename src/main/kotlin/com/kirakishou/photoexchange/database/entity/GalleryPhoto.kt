package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = GalleryPhotoDao.COLLECTION_NAME)
class GalleryPhoto(
	@Id
	@Field(Mongo.Field.ID)
	var id: Long,

	@Indexed(name = Mongo.Index.PHOTO_NAME_INDEX, direction = IndexDirection.DESCENDING)
	@Field(Mongo.Field.PHOTO_NAME)
	var photoName: String,

	@Indexed(name = Mongo.Index.UPLOADED_ON, direction = IndexDirection.DESCENDING)
	@Field(Mongo.Field.UPLOADED_ON)
	val uploadedOn: Long

) {

	fun isEmpty(): Boolean {
		return id <= 0L
	}

	companion object {
		fun empty(): GalleryPhoto {
			return GalleryPhoto(-1L, "", 0L)
		}

		fun create(id: Long, photoName: String, uploadedOn: Long): GalleryPhoto {
			return GalleryPhoto(id, photoName, uploadedOn)
		}
	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val PHOTO_NAME = "photo_name"
			const val UPLOADED_ON = "uploaded_on"

		}

		object Index {
			const val PHOTO_NAME_INDEX = "photo_name_index"
			const val UPLOADED_ON = "uploaded_on_index"
		}
	}
}