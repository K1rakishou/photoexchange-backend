package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = PhotoInfoDao.COLLECTION_NAME)
class PhotoInfo(
	@Id
	@Field(Mongo.Field.PHOTO_ID)
	var photoId: Long,

	@Field(Mongo.Field.EXCHANGE_ID)
	var exchangeId: Long,

	@Indexed(name = Mongo.Index.UPLOADER_USER_ID)
	@Field(Mongo.Field.UPLOADER_USER_ID)
	var uploaderUserId: String,

	@Indexed(name = Mongo.Index.RECEIVER_USER_ID)
	@Field(Mongo.Field.RECEIVER_USER_ID)
	var receiverUserId: String,

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
		fun empty(): PhotoInfo {
			return PhotoInfo(-1L, -1L, "", "", "", false, 0.0, 0.0, 0L)
		}

		fun create(userId: String, photoName: String, isPublic: Boolean, lon: Double, lat: Double, time: Long): PhotoInfo {
			return PhotoInfo(-1L, -1L, userId, "", photoName, isPublic, lon, lat, time)
		}
	}

	object Mongo {
		object Field {
			const val PHOTO_ID = "_id"
			const val EXCHANGE_ID = "exchange_id"
			const val UPLOADER_USER_ID = "uploader_user_id"
			const val RECEIVER_USER_ID = "receiver_user_id"
			const val PHOTO_NAME = "photo_name"
			const val IS_PUBLIC = "is_public"
			const val LONGITUDE = "longitude"
			const val LATITUDE = "latitude"
			const val UPLOADED_ON = "uploaded_on"
		}

		object Index {
			const val UPLOADER_USER_ID = "uploader_user_id_index"
			const val RECEIVER_USER_ID = "receiver_user_id_index"
			const val PHOTO_NAME = "photo_name_index"
			const val UPLOADED_ON = "uploaded_on_index"
		}
	}
}