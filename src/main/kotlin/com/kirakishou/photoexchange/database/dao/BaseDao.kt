package com.kirakishou.photoexchange.database.dao

import org.springframework.data.mongodb.core.ReactiveMongoTemplate

abstract class BaseDao(
  protected val template: ReactiveMongoTemplate
) {
	abstract fun create()
  abstract fun clear()

  fun createCollectionIfNotExists(collectionName: String) {
    if (!template.collectionExists(collectionName).block()!!) {
      template.createCollection(collectionName).block()
    }
  }

  fun dropCollectionIfExists(collectionName: String) {
    if (template.collectionExists(collectionName).block()!!) {
      template.dropCollection(collectionName).block()
    }
  }
}