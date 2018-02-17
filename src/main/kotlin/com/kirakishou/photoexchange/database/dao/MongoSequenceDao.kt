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

    private suspend fun getNextId(sequenceName: String): Long {
        val mongoSequenceMono = template.findAndModify(
                Query.query(Criteria.where("_id").`is`(sequenceName)),
                Update().inc("id", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                MongoSequence::class.java)

        return mongoSequenceMono!!.id
    }

    suspend fun getNextPhotoId(): Long {
       return getNextId(PHOTO_INFO_SEQUENCE_NAME)
    }

    suspend fun getNextPhotoExchangeId(): Long {
        return getNextId(PHOTO_EXCHANGE_INFO_SEQUENCE_NAME)
    }
}