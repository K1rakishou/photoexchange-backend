package com.kirakishou.photoexchange.model.repo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "mongo_sequence")
class MongoSequence(
        @Id
        val sequenceName: String,

        val id: Long
)