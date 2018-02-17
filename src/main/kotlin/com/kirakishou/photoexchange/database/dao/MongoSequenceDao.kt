package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.MongoSequence
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

open class MongoSequenceDao(
	private val template: MongoTemplate
) {
	private val PHOTO_INFO_SEQUENCE_NAME = "photo_info_sequence"
	private val PHOTO_EXCHANGE_INFO_SEQUENCE_NAME = "photo_exchange_info_sequence"

	fun init() {
		if (!template.collectionExists(MongoSequence::class.java)) {
			template.createCollection(MongoSequence::class.java)
		}
	}

	private suspend fun getNextId(sequenceName: String): Long {
		val query = Query()
			.addCriteria(Criteria.where(MongoSequence.Mongo.Field.SEQUENCE_NAME).`is`(sequenceName))

		val update = Update()
			.inc(MongoSequence.Mongo.Field.SEQUENCE_ID, 1)

		val options = FindAndModifyOptions.options().returnNew(true).upsert(true)

		val mongoSequenceMono = template.findAndModify(query, update, options, MongoSequence::class.java)
		return mongoSequenceMono!!.id
	}

	suspend fun getNextPhotoId(): Long {
		return getNextId(PHOTO_INFO_SEQUENCE_NAME)
	}

	suspend fun getNextPhotoExchangeId(): Long {
		return getNextId(PHOTO_EXCHANGE_INFO_SEQUENCE_NAME)
	}
}