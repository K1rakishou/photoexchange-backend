package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.database.entity.GalleryPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

open class GalleryPhotoDao(
  template: ReactiveMongoTemplate
) : BaseDao(template) {
  private val logger = LoggerFactory.getLogger(GalleryPhotoDao::class.java)

  override fun create() {
    createCollectionIfNotExists(COLLECTION_NAME)
  }

  override fun clear() {
    dropCollectionIfExists(COLLECTION_NAME)
  }

  open fun save(galleryPhoto: GalleryPhoto): Mono<Boolean> {
    return template.save(galleryPhoto)
      .map { true }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun findPage(lastUploadedOn: Long, count: Int): Mono<List<GalleryPhoto>> {
    val query = Query().with(Sort(Sort.Direction.DESC, GalleryPhoto.Mongo.Field.ID))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_NAME).ne(""))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.UPLOADED_ON).lt(lastUploadedOn))
      .limit(count)

    return template.find(query, GalleryPhoto::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun countFreshGalleryPhotosSince(time: Long): Mono<Int> {
    val query = Query().with(Sort(Sort.Direction.DESC, GalleryPhoto.Mongo.Field.ID))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_NAME).ne(""))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.UPLOADED_ON).gt(time))

    return template.count(query, GalleryPhoto::class.java)
      .defaultIfEmpty(0L)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(0L)
      .map { it.toInt() }
  }

  /**
   * Transactional
   * */

  open fun deleteByPhotoNameTransactional(txTemplate: ReactiveMongoOperations, photoName: String): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_NAME).`is`(photoName))

    return txTemplate.remove(query, GalleryPhoto::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  /**
   * For test purposes
   * */

  open fun testFindAll(): Mono<List<GalleryPhoto>> {
    return template.findAll(GalleryPhoto::class.java)
      .collectList()
  }

  companion object {
    const val COLLECTION_NAME = "gallery_photo"
  }
}