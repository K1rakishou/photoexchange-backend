package com.kirakishou.photoexchange.model.repo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "mongo_sequence")
class MongoSequence(
	@Id
	@Field(MongoSequence.Mongo.Field.SEQUENCE_NAME)
	val sequenceName: String,

	@Field(MongoSequence.Mongo.Field.SEQUENCE_ID)
	val id: Long
) {
	object Mongo {
		object Field {
			const val SEQUENCE_NAME = "sequence_name"
			const val SEQUENCE_ID = "id"
		}

		object Index {
		}
	}
}