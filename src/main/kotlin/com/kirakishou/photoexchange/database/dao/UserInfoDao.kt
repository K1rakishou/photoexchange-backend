package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.UserInfo
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

class UserInfoDao(
	private val template: MongoTemplate
) : BaseDao {
	private val logger = LoggerFactory.getLogger(UserInfoDao::class.java)

	override fun init() {
		if (!template.collectionExists(UserInfo::class.java)) {
			template.createCollection(UserInfo::class.java)
		}
	}

	suspend fun userIdExists(userId: String): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(UserInfo.Mongo.Field.USER_ID).`is`(userId))

		return template.exists(query, UserInfo::class.java)
	}

	suspend fun save(userInfo: UserInfo): UserInfo {
		try {
			template.save(userInfo)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return UserInfo.empty()
		}

		return userInfo
	}

	fun findManyNotRegistered(userIdList: List<String>): List<UserInfo> {
		val query = Query()
			.addCriteria(Criteria.where(UserInfo.Mongo.Field.USER_ID).`in`(userIdList)
				.andOperator(Criteria.where(UserInfo.Mongo.Field.PASSWORD).ne(""))
			)

		val result = try {
			template.find(query, UserInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return emptyList()
		}

		return result
	}

	companion object {
		const val COLLECTION_NAME = "user_info"
	}
}