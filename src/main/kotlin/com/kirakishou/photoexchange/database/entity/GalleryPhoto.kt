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

	@Indexed(name = Mongo.Index.PHOTO_ID_INDEX, direction = IndexDirection.DESCENDING)
	@Field(Mongo.Field.PHOTO_ID)
	var photoId: Long,

	@Indexed(name = Mongo.Index.UPLOADED_ON, direction = IndexDirection.DESCENDING)
	@Field(Mongo.Field.UPLOADED_ON)
	val uploadedOn: Long

) {

	fun isEmpty(): Boolean {
		return this.id == -1L
	}

	companion object {
		fun empty(): GalleryPhoto {
			return GalleryPhoto(-1L, -1L, 0L)
		}

		fun create(id: Long, photoId: Long, uploadedOn: Long): GalleryPhoto {
			return GalleryPhoto(id, photoId, uploadedOn)
		}
	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val PHOTO_ID = "photo_id"
			const val UPLOADED_ON = "uploaded_on"

		}

		object Index {
			const val UPLOADED_ON = "uploaded_on_index"
			const val PHOTO_ID_INDEX = "photo_id_index"
		}
	}
}