package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.FavouritedPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

open class FavouritedPhotoDao(
	private val template: ReactiveMongoTemplate
) : BaseDao {
	private val logger = LoggerFactory.getLogger(FavouritedPhotoDao::class.java)

	override fun create() {
		if (!template.collectionExists(FavouritedPhoto::class.java).block()) {
			template.createCollection(FavouritedPhoto::class.java).block()
		}
	}

	override fun clear() {
		if (template.collectionExists(FavouritedPhoto::class.java).block()) {
			template.dropCollection(FavouritedPhoto::class.java).block()
		}
	}

	fun favouritePhoto(favouritedPhoto: FavouritedPhoto): Mono<Boolean> {
		return template.save(favouritedPhoto)
			.map { true }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun unfavouritePhoto(userId: String, photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return template.remove(query, FavouritedPhoto::class.java)
			.map { deletionResult -> deletionResult.deletedCount == 1L && deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun findMany(photoIdList: List<Long>): Mono<List<FavouritedPhoto>> {
		val query = Query()
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_ID).`in`(photoIdList))
			.limit(photoIdList.size)

		return template.find(query, FavouritedPhoto::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	fun findMany(userId: String, photoIdList: List<Long>): Mono<List<FavouritedPhoto>> {
		val query = Query()
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.USER_ID).`is`(userId))
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_ID).`in`(photoIdList))
			.limit(photoIdList.size)

		return template.find(query, FavouritedPhoto::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	fun isPhotoFavourited(userId: String, photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return template.exists(query, FavouritedPhoto::class.java)
			.defaultIfEmpty(false)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun deleteByPhotoId(photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))

		return template.remove(query, FavouritedPhoto::class.java)
			.map { deletionResult -> deletionResult.deletedCount == 1L && deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun deleteAll(photoIds: List<Long>): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.ID).`in`(photoIds))
			.limit(photoIds.size)

		return template.remove(query, FavouritedPhoto::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() && deletionResult.deletedCount.toInt() == photoIds.size }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun countByPhotoId(photoId: Long): Mono<Long> {
		val query = Query()
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))

		return template.count(query, FavouritedPhoto::class.java)
			.defaultIfEmpty(0)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(0)
	}

	companion object {
		const val COLLECTION_NAME = "favourited_photo"
	}
}