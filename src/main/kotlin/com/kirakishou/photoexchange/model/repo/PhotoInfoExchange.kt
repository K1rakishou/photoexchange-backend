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
	@Field(Mongo.Field.ID)
	var id: Long,

	@Indexed(name = Mongo.Index.UPLOADER_USER_ID)
	@Field(Mongo.Field.UPLOADER_USER_ID)
	var uploaderUserId: String,

	@Indexed(name = Mongo.Index.RECEIVER_USER_ID)
	@Field(Mongo.Field.RECEIVER_USER_ID)
	var receiverUserId: String,

	@Field(Mongo.Field.UPLOADER_OK_TIME)
	var uploaderOkTime: Long,

	@Field(Mongo.Field.RECEIVER_OK_TIME)
	var receiverOkTime: Long,

	@Indexed(name = Mongo.Index.CREATED_ON, direction = IndexDirection.DESCENDING)
	@Field(Mongo.Field.CREATED_ON)
	var createdOn: Long
) {

	fun isEmpty(): Boolean {
		return id == -1L
	}

	fun isExchangeSuccessful(): Boolean {
		return (uploaderOkTime > 0L) &&
			(receiverOkTime > 0L) &&
			uploaderUserId.isNotEmpty() &&
			receiverUserId.isNotEmpty()
	}

	companion object {
		fun empty(): PhotoInfoExchange {
			return PhotoInfoExchange(-1L, "", "", 0L, 0L, 0L)
		}

		fun create(uploaderUserId: String): PhotoInfoExchange {
			return PhotoInfoExchange(-1L, uploaderUserId, "", 0L, 0L, TimeUtils.getTimeFast())
		}


	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val UPLOADER_USER_ID = "uploader_user_id"
			const val RECEIVER_USER_ID = "receiver_user_id"
			const val UPLOADER_OK_TIME = "uploader_ok_time"
			const val RECEIVER_OK_TIME = "receiver_ok_time"
			const val CREATED_ON = "created_on"
		}

		object Index {
			const val UPLOADER_USER_ID = "uploader_user_id_index"
			const val RECEIVER_USER_ID = "receiver_user_id_index"
			const val CREATED_ON = "created_on_index"
		}
	}
}