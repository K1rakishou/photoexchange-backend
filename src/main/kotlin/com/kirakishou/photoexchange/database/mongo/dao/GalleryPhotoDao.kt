package com.kirakishou.photoexchange.database.mongo.dao

import com.kirakishou.photoexchange.database.mongo.entity.GalleryPhoto
import org.slf4j.LoggerFactory
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
    return reactiveTemplate.save(galleryPhoto)
      .map { true }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun findPage(lastUploadedOn: Long, count: Int): Mono<List<GalleryPhoto>> {
    val query = Query().with(Sort(Sort.Direction.DESC, GalleryPhoto.Mongo.Field.ID))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_NAME).ne(""))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.UPLOADED_ON).lt(lastUploadedOn))
      .limit(count)

    return reactiveTemplate.find(query, GalleryPhoto::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun countFreshGalleryPhotosSince(time: Long): Mono<Int> {
    val query = Query().with(Sort(Sort.Direction.DESC, GalleryPhoto.Mongo.Field.ID))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_NAME).ne(""))
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.UPLOADED_ON).gt(time))

    return reactiveTemplate.count(query, GalleryPhoto::class.java)
      .defaultIfEmpty(0L)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(0L)
      .map { it.toInt() }
  }

  open fun deleteByPhotoName(photoName: String, template: ReactiveMongoOperations = reactiveTemplate): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.PHOTO_NAME).`is`(photoName))

    return template.remove(query, GalleryPhoto::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  /**
   * For test purposes
   * */

  open fun testFindAll(): Mono<List<GalleryPhoto>> {
    return reactiveTemplate.findAll(GalleryPhoto::class.java)
      .collectList()
  }

  companion object {
    const val COLLECTION_NAME = "gallery_photo"
  }
}