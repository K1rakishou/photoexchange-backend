package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = GalleryPhotoDao.COLLECTION_NAME)
class GalleryPhoto(
	@Id
	@Field(Mongo.Field.PHOTO_ID)
	var photoId: Long,

	@Indexed(name = Mongo.Index.UPLOADED_ON, direction = IndexDirection.DESCENDING)
	@Field(Mongo.Field.UPLOADED_ON)
	val uploadedOn: Long
) {

	companion object {
		fun create(photoId: Long, uploadedOn: Long): GalleryPhoto {
			return GalleryPhoto(photoId, uploadedOn)
		}
	}

	object Mongo {
		object Field {
			const val PHOTO_ID = "_id"
			const val UPLOADED_ON = "uploaded_on"
		}

		object Index {
			const val UPLOADED_ON = "uploaded_on_index"
		}
	}
}