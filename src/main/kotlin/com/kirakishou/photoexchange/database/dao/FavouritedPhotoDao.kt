package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.database.entity.FavouritedPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

open class FavouritedPhotoDao(
  template: ReactiveMongoTemplate
) : BaseDao(template) {
  private val logger = LoggerFactory.getLogger(FavouritedPhotoDao::class.java)

  override fun create() {
    createCollectionIfNotExists(COLLECTION_NAME)
  }

  override fun clear() {
    dropCollectionIfExists(COLLECTION_NAME)
  }

  open fun favouritePhoto(favouritedPhoto: FavouritedPhoto): Mono<Boolean> {
    return reactiveTemplate.save(favouritedPhoto)
      .map { true }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun unfavouritePhoto(userId: String, photoName: String): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_NAME).`is`(photoName))
      .addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.USER_ID).`is`(userId))

    return reactiveTemplate.remove(query, FavouritedPhoto::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun findManyFavouritesByPhotoNameList(userId: String, photoNameList: List<String>): Mono<List<FavouritedPhoto>> {
    val query = Query()
      .addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.USER_ID).`is`(userId))
      .addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_NAME).`in`(photoNameList))
      .limit(photoNameList.size)

    return reactiveTemplate.find(query, FavouritedPhoto::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun isPhotoFavourited(userId: String, photoName: String): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_NAME).`is`(photoName))
      .addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.USER_ID).`is`(userId))

    return reactiveTemplate.exists(query, FavouritedPhoto::class.java)
      .defaultIfEmpty(false)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun countFavouritesByPhotoName(photoName: String): Mono<Long> {
    val query = Query()
      .addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_NAME).`is`(photoName))

    return reactiveTemplate.count(query, FavouritedPhoto::class.java)
      .defaultIfEmpty(0)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(0)
  }

  open fun deleteFavouriteByPhotoName(photoName: String, template: ReactiveMongoOperations = reactiveTemplate): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_NAME).`is`(photoName))

    return template.remove(query, FavouritedPhoto::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  /**
   * For test purposes
   * */

  open fun testFindAll(): Mono<List<FavouritedPhoto>> {
    return reactiveTemplate.findAll(FavouritedPhoto::class.java)
      .collectList()
  }

  companion object {
    const val COLLECTION_NAME = "favourited_photo"
  }
}