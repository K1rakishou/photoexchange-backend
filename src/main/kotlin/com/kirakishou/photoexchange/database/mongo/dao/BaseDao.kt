package com.kirakishou.photoexchange.database.mongo.dao

abstract class BaseDao(
  protected val reactiveTemplate: ReactiveMongoTemplate
) {
	abstract fun create()
  abstract fun clear()

  fun createCollectionIfNotExists(collectionName: String) {
    if (!reactiveTemplate.collectionExists(collectionName).block()!!) {
      reactiveTemplate.createCollection(collectionName).block()
    }
  }

  fun dropCollectionIfExists(collectionName: String) {
    if (reactiveTemplate.collectionExists(collectionName).block()!!) {
      reactiveTemplate.dropCollection(collectionName).block()
    }
  }
}