package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

open class PhotoInfoExchangeDao(
	private val template: MongoTemplate
) {
	private val logger = LoggerFactory.getLogger(PhotoInfoExchangeDao::class.java)

	fun init() {
		if (!template.collectionExists(PhotoInfoExchange::class.java)) {
			template.createCollection(PhotoInfoExchange::class.java)
		}
	}

	suspend fun save(photoInfoExchange: PhotoInfoExchange): PhotoInfoExchange {
		try {
			template.save(photoInfoExchange)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return PhotoInfoExchange.empty()
		}

		return photoInfoExchange
	}

	suspend fun findAllByIdList(ids: List<Long>, sortByDesc: Boolean = true): List<PhotoInfoExchange> {
		val query = if (sortByDesc) {
			Query().with(Sort(Sort.Direction.DESC, PhotoInfoExchange.Mongo.Field.CREATED_ON))
				.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`in`(ids))
		} else {
			Query().with(Sort(Sort.Direction.ASC, PhotoInfoExchange.Mongo.Field.CREATED_ON))
				.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`in`(ids))
		}

		val result = try {
			template.find(query, PhotoInfoExchange::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			emptyList<PhotoInfoExchange>()
		}

		return result
	}

	suspend fun findById(id: Long): PhotoInfoExchange {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`is`(id))

		val result = try {
			template.findOne(query, PhotoInfoExchange::class.java) ?: PhotoInfoExchange.empty()
		} catch (error: Throwable) {
			logger.error("DB error", error)
			PhotoInfoExchange.empty()
		}

		return result
	}

	suspend fun countAllUploadedByIdList(ids: List<Long>): Long {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.UPLOADER_USER_ID).`in`(ids))

		val result = try {
			template.count(query, PhotoInfoExchange::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			0L
		}

		return result
	}

	suspend fun countAllReceivedByIdList(ids: List<Long>): Long {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.RECEIVER_USER_ID).`in`(ids))

		val result = try {
			template.count(query, PhotoInfoExchange::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			0L
		}

		return result
	}

	fun updateSetPhotoSuccessfullyDelivered(exchangeId: Long, isUploader: Boolean): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.ID).`is`(exchangeId))
			.limit(1)

		val update = if (isUploader) {
			Update().set(PhotoInfoExchange.Mongo.Field.UPLOADER_SENT_OK, true)
		} else {
			Update().set(PhotoInfoExchange.Mongo.Field.RECEIVER_SENT_OK, true)
		}

		val result = try {
			val updateResult = template.updateFirst(query, update, PhotoInfoExchange::class.java)
			updateResult.wasAcknowledged() && updateResult.modifiedCount == 1L
		} catch (error: Throwable) {
			logger.error("DB error", error)
			false
		}

		return result
	}

	suspend fun tryDoExchangeWithOldestPhoto(receiverUserId: String): PhotoInfoExchange {
		val query = Query().with(Sort(Sort.Direction.ASC, PhotoInfoExchange.Mongo.Field.CREATED_ON))
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.UPLOADER_USER_ID).ne("")
				.andOperator(Criteria.where(PhotoInfoExchange.Mongo.Field.UPLOADER_USER_ID).ne(receiverUserId)))
			.addCriteria(Criteria.where(PhotoInfoExchange.Mongo.Field.RECEIVER_USER_ID).`is`(""))
			.limit(1)

		val update = Update()
			.set(PhotoInfoExchange.Mongo.Field.RECEIVER_USER_ID, receiverUserId)

		val options = FindAndModifyOptions.options().returnNew(true)

		val result = try {
			template.findAndModify(query, update, options, PhotoInfoExchange::class.java) ?: PhotoInfoExchange.empty()
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return PhotoInfoExchange.empty()
		}

		return result
	}

	companion object {
		const val COLLECTION_NAME = "photo_info_exchange"
	}
}