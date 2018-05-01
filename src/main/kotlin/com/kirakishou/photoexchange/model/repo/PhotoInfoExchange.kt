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

	@Field(Mongo.Field.UPLOADER_SENT_OK)
	var uploaderSentOk: Boolean,

	@Field(Mongo.Field.RECEIVER_SENT_OK)
	var receiverSentOk: Boolean,

	@Indexed(name = Mongo.Index.CREATED_ON, direction = IndexDirection.DESCENDING)
	@Field(Mongo.Field.CREATED_ON)
	var createdOn: Long
) {

	fun isEmpty(): Boolean {
		return id == -1L
	}

	fun isExchangeSuccessful(): Boolean {
		return uploaderUserId.isNotEmpty() &&
			receiverUserId.isNotEmpty()
	}

	companion object {
		fun empty(): PhotoInfoExchange {
			return PhotoInfoExchange(-1L, "", "", false, false, 0L)
		}

		fun create(photoExchangeId: Long, uploaderUserId: String): PhotoInfoExchange {
			return PhotoInfoExchange(photoExchangeId, uploaderUserId, "", false, false, TimeUtils.getTimeFast())
		}
	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val UPLOADER_USER_ID = "uploader_user_id"
			const val RECEIVER_USER_ID = "receiver_user_id"
			const val UPLOADER_SENT_OK = "uploader_sent_ok"
			const val RECEIVER_SENT_OK = "receiver_sent_ok"
			const val CREATED_ON = "created_on"
		}

		object Index {
			const val UPLOADER_USER_ID = "uploader_user_id_index"
			const val RECEIVER_USER_ID = "receiver_user_id_index"
			const val CREATED_ON = "created_on_index"
		}
	}
}