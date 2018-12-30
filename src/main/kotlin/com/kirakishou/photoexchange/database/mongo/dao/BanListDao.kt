package com.kirakishou.photoexchange.database.mongo.dao

import com.kirakishou.photoexchange.database.mongo.entity.BanEntry
import org.slf4j.LoggerFactory
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
    return reactiveTemplate.save(banEntry)
      .map { true }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun saveMany(banEntryList: List<BanEntry>): Mono<Boolean> {
    return reactiveTemplate.insertAll(banEntryList)
      .collectList()
      .map { true }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun find(ipHash: String): Mono<BanEntry> {
    val query = Query()
      .addCriteria(Criteria.where(BanEntry.Mongo.Field.IP_HASH).`is`(ipHash))

    return reactiveTemplate.findOne(query, BanEntry::class.java)
      .defaultIfEmpty(BanEntry.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(BanEntry.empty())
  }

  companion object {
    const val COLLECTION_NAME = "ban_list"
  }
}