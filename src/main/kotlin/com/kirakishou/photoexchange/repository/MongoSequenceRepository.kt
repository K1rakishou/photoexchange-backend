package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.MongoSequence
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

open class MongoSequenceRepository(
    private val template: MongoTemplate
) {
    private val SEQUENCE_NAME = "photo_info_sequence"

    suspend fun getNextId(): Long {
        val mongoSequenceMono = template.findAndModify(
                Query.query(Criteria.where("_id").`is`(SEQUENCE_NAME)),
                Update().inc("id", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                MongoSequence::class.java)

        return mongoSequenceMono!!.id
    }
}