package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.UserInfo
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

open class UserInfoDao(
	private val template: ReactiveMongoTemplate
) : BaseDao {
	private val logger = LoggerFactory.getLogger(UserInfoDao::class.java)

	override fun create() {
		if (!template.collectionExists(UserInfo::class.java).block()) {
			template.createCollection(UserInfo::class.java).block()
		}
	}

	override fun clear() {
		if (template.collectionExists(UserInfo::class.java).block()) {
			template.dropCollection(UserInfo::class.java).block()
		}
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

	fun findManyNotRegistered(userIdList: List<String>): Mono<List<UserInfo>> {
		val query = Query()
			.addCriteria(Criteria.where(UserInfo.Mongo.Field.USER_ID).`in`(userIdList)
				.andOperator(Criteria.where(UserInfo.Mongo.Field.PASSWORD).ne(""))
			)

		return template.find(query, UserInfo::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	companion object {
		const val COLLECTION_NAME = "user_info"
	}
}