package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import reactor.core.publisher.Mono

open class PhotoInfoExchangeDao(
	private val template: ReactiveMongoTemplate
) : BaseDao {
	private val logger = LoggerFactory.getLogger(PhotoInfoExchangeDao::class.java)

	override fun create() {
		if (!template.collectionExists(PhotoInfoExchange::class.java).block()) {
			template.createCollection(PhotoInfoExchange::class.java).block()
		}
	}

	override fun clear() {
		if (template.collectionExists(PhotoInfoExchange::class.java).block()) {
			template.dropCollection(PhotoInfoExchange::class.java).block()
		}
	}

	fun save(photoInfoExchange: PhotoInfoExchange): Mono<PhotoInfoExchange> {
		return template.save(photoInfoExchange)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(PhotoInfoExchange.empty())
	}

	fun findManyByIdList(ids: List<Long>, sortByDesc: Boolean = true): Mono<List<PhotoInfoExchange>> {
		val query = if (sortByDesc) {
			Query().with(Sort(Sort.Direction.DESC, PhotoInfoExchange.Mongo.Field.CREATED_ON))
				.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`in`(ids))
		} else {
			Query().with(Sort(Sort.Direction.ASC, PhotoInfoExchange.Mongo.Field.CREATED_ON))
				.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`in`(ids))
		}

		return template.find(query, PhotoInfoExchange::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	fun findById(id: Long): Mono<PhotoInfoExchange> {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`is`(id))

		return template.findOne(query, PhotoInfoExchange::class.java)
			.defaultIfEmpty(PhotoInfoExchange.empty())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(PhotoInfoExchange.empty())
	}

	open fun tryDoExchangeWithOldestPhoto(receiverPhotoId: Long, receiverUserId: String): Mono<PhotoInfoExchange> {
		val query = Query().with(Sort(Sort.Direction.ASC, PhotoInfoExchange.Mongo.Field.CREATED_ON))
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.UPLOADER_USER_ID).ne("")
				.andOperator(Criteria.where(PhotoInfoExchange.Mongo.Field.UPLOADER_USER_ID).ne(receiverUserId)))
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.RECEIVER_USER_ID).`is`(""))
			.limit(1)

		val update = Update()
			.set(PhotoInfoExchange.Mongo.Field.RECEIVER_USER_ID, receiverUserId)
			.set(PhotoInfoExchange.Mongo.Field.RECEIVER_PHOTO_ID, receiverPhotoId)

		val options = FindAndModifyOptions.options().returnNew(true)

		return template.findAndModify(query, update, options, PhotoInfoExchange::class.java)
			.defaultIfEmpty(PhotoInfoExchange.empty())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(PhotoInfoExchange.empty())
	}

	fun updateResetReceiverInfo(exchangeId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`is`(exchangeId))

		val update = Update()
			.set(PhotoInfoExchange.Mongo.Field.RECEIVER_PHOTO_ID, -1)
			.set(PhotoInfoExchange.Mongo.Field.RECEIVER_USER_ID, "")

		return template.updateFirst(query, update, PhotoInfoExchange::class.java)
			.map { updateResult -> updateResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun deleteById(exchangeId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`is`(exchangeId))

		return template.remove(query, PhotoInfoExchange::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun deleteAll(exchangeIds: List<Long>): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`in`(exchangeIds))
			.limit(exchangeIds.size)

		return template.remove(query, PhotoInfoExchange::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	companion object {
		const val COLLECTION_NAME = "photo_info_exchange"
	}
}