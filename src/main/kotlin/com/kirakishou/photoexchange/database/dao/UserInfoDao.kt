package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.database.entity.UserInfo
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
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

	fun save(userInfo: UserInfo): Mono<UserInfo> {
		return template.save(userInfo)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(UserInfo.empty())
	}

  fun userIdExists(userId: String): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(UserInfo.Mongo.Field.USER_ID).`is`(userId))

    return template.exists(query, UserInfo::class.java)
      .defaultIfEmpty(false)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun getUser(userId: String): Mono<UserInfo> {
    val query = Query()
      .addCriteria(Criteria.where(UserInfo.Mongo.Field.USER_ID).`is`(userId))

    return template.findOne(query, UserInfo::class.java)
      .defaultIfEmpty(UserInfo.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(UserInfo.empty())
  }

	fun updateFirebaseToken(userId: String, token: String): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(UserInfo.Mongo.Field.USER_ID).`is`(userId))

    val update = Update()
      .set(UserInfo.Mongo.Field.FIREBASE_TOKEN, token)

    return template.updateFirst(query, update, UserInfo::class.java)
      .map { updateResult -> updateResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

	companion object {
		const val COLLECTION_NAME = "user_info"
	}
}