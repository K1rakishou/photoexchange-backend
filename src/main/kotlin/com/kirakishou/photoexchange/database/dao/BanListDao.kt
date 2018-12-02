package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.database.entity.BanEntry
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

open class BanListDao(
  template: ReactiveMongoTemplate
) : BaseDao(template) {
  private val logger = LoggerFactory.getLogger(BanListDao::class.java)

  override fun create() {
    createCollectionIfNotExists(COLLECTION_NAME)
  }

  override fun clear() {
    dropCollectionIfExists(COLLECTION_NAME)
  }

  fun save(banEntry: BanEntry): Mono<Boolean> {
    return template.save(banEntry)
      .map { true }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun find(ipHash: String): Mono<BanEntry> {
    val query = Query()
      .addCriteria(Criteria.where(BanEntry.Mongo.Field.IP_HASH).`is`(ipHash))

    return template.findOne(query, BanEntry::class.java)
      .defaultIfEmpty(BanEntry.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(BanEntry.empty())
  }

  companion object {
    const val COLLECTION_NAME = "ban_list"
  }
}