package com.kirakishou.photoexchange.database.mongo.entity

import com.kirakishou.photoexchange.database.mongo.dao.MongoSequenceDao

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