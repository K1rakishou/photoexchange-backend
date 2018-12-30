package com.kirakishou.photoexchange.database.mongo.entity

import com.kirakishou.photoexchange.database.mongo.dao.ReportedPhotoDao

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
	var userId: String
) {

	fun isEmpty(): Boolean {
		return id <= 0L
	}

	companion object {
		fun empty(): ReportedPhoto {
			return ReportedPhoto(-1L, "", "")
		}

		fun create(id: Long, photoName: String, userId: String): ReportedPhoto {
			return ReportedPhoto(id, photoName, userId)
		}
	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val PHOTO_NAME = "photo_name"
			const val USER_ID = "user_id"
		}

		object Index {
			const val PHOTO_NAME_INDEX = "photo_name_index"
			const val USER_ID_INDEX = "user_id_index"
		}
	}
}