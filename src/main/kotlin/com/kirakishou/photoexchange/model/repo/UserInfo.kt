package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.database.dao.UserInfoDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = UserInfoDao.COLLECTION_NAME)
class UserInfo(

	@Id
	@Field(Mongo.Field.ID)
	var id: Long,

	@Indexed(name = Mongo.Index.USER_ID)
	@Field(Mongo.Field.USER_ID)
	var userId: String,

	@Field(Mongo.Field.PASSWORD)
	var password: String
) {
	fun isEmpty(): Boolean {
		return id == -1L
	}

	companion object {
		fun empty(): UserInfo {
			return UserInfo(-1L, "", "")
		}

		fun create(userId: String, password: String = ""): UserInfo {
			return UserInfo(-1L, userId, password)
		}
	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val USER_ID = "user_id"
			const val PASSWORD = "password"
		}

		object Index {
			const val USER_ID = "user_id_index"
		}
	}
}