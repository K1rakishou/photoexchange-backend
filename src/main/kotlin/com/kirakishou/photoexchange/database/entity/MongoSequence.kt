package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = MongoSequenceDao.COLLECTION_NAME)
class MongoSequence(
	@Id
	@Field(Mongo.Field.SEQUENCE_NAME)
	val sequenceName: String,

	@Field(Mongo.Field.SEQUENCE_ID)
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