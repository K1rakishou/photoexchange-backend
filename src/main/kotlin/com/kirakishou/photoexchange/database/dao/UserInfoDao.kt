package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.UserInfo
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

open class UserInfoDao(
	template: ReactiveMongoTemplate
) : BaseDao(template) {
	private val logger = LoggerFactory.getLogger(UserInfoDao::class.java)

	override fun create() {
		createCollectionIfNotExists(COLLECTION_NAME)
	}

	override fun clear() {
		dropCollectionIfExists(COLLECTION_NAME)
	}

	fun userIdExists(userId: String): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(UserInfo.Mongo.Field.USER_ID).`is`(userId))

		return template.exists(query, UserInfo::class.java)
			.defaultIfEmpty(false)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun save(userInfo: UserInfo): Mono<UserInfo> {
		return template.save(userInfo)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(UserInfo.empty())
	}

	companion object {
		const val COLLECTION_NAME = "user_info"
	}
}