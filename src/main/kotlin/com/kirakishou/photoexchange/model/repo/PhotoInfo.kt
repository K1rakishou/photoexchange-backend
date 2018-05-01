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
	val uploadedOn: Long,

	@Field(Mongo.Field.FAVOURITES_COUNT)
	val favouritesCount: Long,

	@Field(Mongo.Field.REPORTS_COUNT)
	val reportsCount: Long
) {
	fun isEmpty(): Boolean {
		return photoId == -1L
	}

	companion object {
		fun empty(): PhotoInfo {
			return PhotoInfo(-1L, -1L, "", "", false, 0.0, 0.0, 0L, 0L, 0L)
		}

		fun create(userId: String, photoName: String, isPublic: Boolean, lon: Double, lat: Double, time: Long): PhotoInfo {
			return PhotoInfo(-1L, -1L, userId, photoName, isPublic, lon, lat, time, 0L, 0L)
		}
	}

	object Mongo {
		object Field {
			const val PHOTO_ID = "_id"
			const val EXCHANGE_ID = "exchange_id"
			const val USER_ID = "user_id"
			const val PHOTO_NAME = "photo_name"
			const val IS_PUBLIC = "is_public"
			const val LONGITUDE = "longitude"
			const val LATITUDE = "latitude"
			const val UPLOADED_ON = "uploaded_on"
			const val FAVOURITES_COUNT = "favourites_count"
			const val REPORTS_COUNT = "reports_count"
		}

		object Index {
			const val USER_ID = "user_id_index"
			const val PHOTO_NAME = "photo_name_index"
			const val UPLOADED_ON = "uploaded_on_index"
		}
	}
}