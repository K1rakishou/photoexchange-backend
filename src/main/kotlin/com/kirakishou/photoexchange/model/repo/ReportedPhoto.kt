package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.database.dao.ReportedPhotoDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = ReportedPhotoDao.COLLECTION_NAME)
class ReportedPhoto(
	@Id
	@Field(Mongo.Field.ID)
	var id: Long,

  @Field(Mongo.Field.PHOTO_NAME)
  @Indexed(name = Mongo.Index.PHOTO_NAME_INDEX)
	var photoName: String,

	@Indexed(name = Mongo.Index.USER_ID_INDEX)
	@Field(Mongo.Field.USER_ID)
	var userId: String,

	@Indexed(name = Mongo.Index.PHOTO_ID_INDEX)
	@Field(Mongo.Field.PHOTO_ID)
	var photoId: Long
) {

	companion object {
		fun create(id: Long, photoName: String, userId: String, photoId: Long): ReportedPhoto {
			return ReportedPhoto(id, photoName, userId, photoId)
		}
	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val PHOTO_NAME = "photo_name"
			const val USER_ID = "user_id"
			const val PHOTO_ID = "photo_id"
		}

		object Index {
			const val PHOTO_NAME_INDEX = "photo_name_index"
			const val USER_ID_INDEX = "user_id_index"
			const val PHOTO_ID_INDEX = "photo_id_index"
		}
	}
}