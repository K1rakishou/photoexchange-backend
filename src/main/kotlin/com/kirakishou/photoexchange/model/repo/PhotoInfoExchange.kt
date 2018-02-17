package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.util.TimeUtils
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = PhotoInfoExchangeDao.COLLECTION_NAME)
class PhotoInfoExchange(
	@Id
	@Field(Mongo.Field.EXCHANGE_ID)
	var exchangeId: Long,

	@Indexed(name = Mongo.Index.UPLOADER_PHOTO_INFO_ID)
	@Field(Mongo.Field.UPLOADER_PHOTO_INFO_ID)
	var uploaderPhotoInfoId: Long,

	@Indexed(name = Mongo.Index.RECEIVER_PHOTO_INFO_ID)
	@Field(Mongo.Field.RECEIVER_PHOTO_INFO_ID)
	var receiverPhotoInfoId: Long,

	@Field(Mongo.Field.UPLOADER_OK)
	var uploaderOk: Boolean,

	@Field(Mongo.Field.RECEIVER_OK)
	var receiverOk: Boolean,

	@Indexed(name = Mongo.Index.CREATED_ON, direction = IndexDirection.DESCENDING)
	@Field(Mongo.Field.CREATED_ON)
	var createdOn: Long
) {

	fun isEmpty(): Boolean {
		return exchangeId == -1L
	}

	fun isExchangeSuccessful(): Boolean {
		return uploaderOk && receiverOk && (uploaderPhotoInfoId > 0L) && (receiverPhotoInfoId > 0L)
	}

	companion object {
		fun empty(): PhotoInfoExchange {
			return PhotoInfoExchange(-1L, 0L, 0L, false, false, 0L)
		}

		fun create(uploaderPhotoInfoId: Long): PhotoInfoExchange {
			return PhotoInfoExchange(-1L, uploaderPhotoInfoId, 0L, false, false, TimeUtils.getTimeFast())
		}
	}

	object Mongo {
		object Field {
			const val EXCHANGE_ID = "exchange_id"
			const val UPLOADER_PHOTO_INFO_ID = "uploader_photo_info_id"
			const val RECEIVER_PHOTO_INFO_ID = "receiver_photo_info_id"
			const val UPLOADER_OK = "uploader_ok"
			const val RECEIVER_OK = "receiver_ok"
			const val CREATED_ON = "created_on"
		}

		object Index {
			const val UPLOADER_PHOTO_INFO_ID = "uploader_photo_info_id_index"
			const val RECEIVER_PHOTO_INFO_ID = "receiver_photo_info_id_index"
			const val CREATED_ON = "created_on_index"
		}
	}
}