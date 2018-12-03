package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.database.entity.GalleryPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
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

  fun save(galleryPhoto: GalleryPhoto): Mono<Boolean> {
    return template.save(galleryPhoto)
      .map { true }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun findPage(lastUploadedOn: Long, count: Int): Mono<List<GalleryPhoto>> {
    val query = Query().with(Sort(Sort.Direction.DESC, GalleryPhoto.Mongo.Field.ID))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_ID).gt(0L))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.UPLOADED_ON).lt(lastUploadedOn))
      .limit(count)

    return template.find(query, GalleryPhoto::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  fun countFreshGalleryPhotosSince(time: Long): Mono<Int> {
    val query = Query().with(Sort(Sort.Direction.DESC, GalleryPhoto.Mongo.Field.ID))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_ID).gt(0L))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.UPLOADED_ON).gt(time))

    return template.count(query, GalleryPhoto::class.java)
      .defaultIfEmpty(0L)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(0L)
      .map { it.toInt() }
  }

  fun deleteById(photoId: Long): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))

    return template.remove(query, GalleryPhoto::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun deleteAll(photoIds: List<Long>): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_ID).`in`(photoIds))
      .limit(photoIds.size)

    return template.remove(query, GalleryPhoto::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  companion object {
    const val COLLECTION_NAME = "gallery_photo"
  }
}