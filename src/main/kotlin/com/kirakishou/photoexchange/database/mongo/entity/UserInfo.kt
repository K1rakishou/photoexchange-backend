package com.kirakishou.photoexchange.database.mongo.entity

import com.kirakishou.photoexchange.database.mongo.dao.UserInfoDao

@Document(collection = UserInfoDao.COLLECTION_NAME)
class UserInfo(

	@Id
	@Field(Mongo.Field.ID)
	var id: Long,

	@Indexed(name = Mongo.Index.USER_ID)
	@Field(Mongo.Field.USER_ID)
	var userId: String,

  @Field(Mongo.Field.FIREBASE_TOKEN)
	var firebaseToken: String,

	@Field(Mongo.Field.PASSWORD)
	var password: String
) {
	fun isEmpty(): Boolean {
		return id == -1L
	}

	companion object {
		fun empty(): UserInfo {
			return UserInfo(-1L, "", "", "")
		}

		fun create(userId: String, password: String = ""): UserInfo {
			return UserInfo(-1L, userId, "", password)
		}
	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val USER_ID = "user_id"
      const val FIREBASE_TOKEN = "firebase_token"
			const val PASSWORD = "password"
		}

		object Index {
			const val USER_ID = "user_id_index"
		}
	}
}