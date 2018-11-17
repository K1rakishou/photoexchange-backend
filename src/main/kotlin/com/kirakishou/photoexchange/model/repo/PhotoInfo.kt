package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = PhotoInfoDao.COLLECTION_NAME)
data class PhotoInfo(
	@Id
	@Field(Mongo.Field.PHOTO_ID)
	var photoId: Long,

  @Indexed(name = Mongo.Index.EXCHANGED_PHOTO_ID)
  @Field(Mongo.Field.EXCHANGED_PHOTO_ID)
  var exchangedPhotoId: Long,

	@Field(Mongo.Field.LOCATION_MAP_ID)
	val locationMapId: Long,

	@Indexed(name = Mongo.Index.USER_ID)
	@Field(Mongo.Field.USER_ID)
	var userId: String,

	@Indexed(name = Mongo.Index.PHOTO_NAME)
	@Field(Mongo.Field.PHOTO_NAME)
	val photoName: String,

	@Field(Mongo.Field.IS_PUBLIC)
	val isPublic: Boolean,

	@Field(Mongo.Field.LONGITUDE)
	val lon: Double,

	@Field(Mongo.Field.LATITUDE)
	val lat: Double,

	@Indexed(name = Mongo.Index.UPLOADED_ON, direction = IndexDirection.DESCENDING)
	@Field(Mongo.Field.UPLOADED_ON)
	val uploadedOn: Long
) {
	fun isEmpty(): Boolean {
		return photoId == -1L
	}

	companion object {

		const val EMPTY_PHOTO_ID = -1L
		const val EMPTY_LOCATION_MAP_ID = -1L

		fun empty(): PhotoInfo {
			return PhotoInfo(EMPTY_PHOTO_ID, EMPTY_PHOTO_ID, EMPTY_LOCATION_MAP_ID, "", "", false, 0.0, 0.0, 0L)
		}

		fun create(userId: String, photoName: String, isPublic: Boolean, lon: Double, lat: Double, time: Long): PhotoInfo {
			return PhotoInfo(EMPTY_PHOTO_ID, EMPTY_PHOTO_ID, EMPTY_LOCATION_MAP_ID, userId, photoName, isPublic, lon, lat, time)
		}
	}

	object Mongo {
		object Field {
			const val PHOTO_ID = "photo_id"
			const val EXCHANGED_PHOTO_ID = "exchanged_photo_id"
			const val USER_ID = "user_id"
			const val PHOTO_NAME = "photo_name"
			const val LOCATION_MAP_ID = "location_map_id"
			const val IS_PUBLIC = "is_public"
			const val LONGITUDE = "longitude"
			const val LATITUDE = "latitude"
			const val UPLOADED_ON = "uploaded_on"
		}

		object Index {
      const val EXCHANGED_PHOTO_ID = "exchanged_photo_id_index"
			const val USER_ID = "user_id_index"
			const val PHOTO_NAME = "photo_name_index"
			const val UPLOADED_ON = "uploaded_on_index"
		}
	}
}