package com.kirakishou.photoexchange.database.mongo.entity

@Document(collection = FavouritedPhotoDao.COLLECTION_NAME)
data class FavouritedPhoto(
	@Id
	@Field(Mongo.Field.ID)
	var id: Long,

  @Indexed(name = Mongo.Index.PHOTO_NAME_INDEX)
  @Field(Mongo.Field.PHOTO_NAME)
  var photoName: String,

	@Indexed(name = Mongo.Index.USER_ID_INDEX)
	@Field(Mongo.Field.USER_ID)
	var userId: String
) {

	companion object {
		fun create(id: Long, photoName: String, userId: String): FavouritedPhoto {
			return FavouritedPhoto(id, photoName, userId)
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