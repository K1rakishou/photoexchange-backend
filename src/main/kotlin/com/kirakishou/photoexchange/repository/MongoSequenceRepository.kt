package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.MongoSequence
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import reactor.core.publisher.Mono

open class MongoSequenceRepository(private val template: ReactiveMongoTemplate) {

    fun getNextId(sequenceName: String): Mono<Long> {
        val mongoSequenceMono = template.findAndModify(
                Query.query(Criteria.where("_id").`is`(sequenceName)),
                Update().inc("id", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                MongoSequence::class.java)

        return mongoSequenceMono
                .map { it.id }
    }
}