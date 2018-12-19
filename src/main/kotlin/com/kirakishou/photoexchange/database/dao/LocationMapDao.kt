package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.database.entity.LocationMap
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import reactor.core.publisher.Mono

open class LocationMapDao(
	template: ReactiveMongoTemplate
) : BaseDao(template) {
	private val logger = LoggerFactory.getLogger(LocationMapDao::class.java)

	override fun create() {
		createCollectionIfNotExists(COLLECTION_NAME)
	}

	override fun clear() {
		dropCollectionIfExists(COLLECTION_NAME)
	}

	open fun save(locationMap: LocationMap): Mono<LocationMap> {
		return reactiveTemplate.save(locationMap)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(LocationMap.empty())
	}

	open fun findOldest(count: Int, currentTime: Long): Mono<List<LocationMap>> {
		val query = Query().with(Sort(Sort.Direction.ASC, LocationMap.Mongo.Field.ID))
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.NEXT_ATTEMPT_TIME).lt(currentTime))
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.MAP_STATUS).`is`(LocationMap.MapStatus.Empty.value))
			.limit(count)

		return reactiveTemplate.find(query, LocationMap::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	open fun updateSetMapReady(photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`is`(photoId))

		val update = Update()
			.set(LocationMap.Mongo.Field.MAP_STATUS, LocationMap.MapStatus.Ready.value)

		return reactiveTemplate.updateFirst(query, update, LocationMap::class.java)
			.map { updateResult -> updateResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	open fun updateSetMapAnonymous(photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`is`(photoId))

		val update = Update()
			.set(LocationMap.Mongo.Field.MAP_STATUS, LocationMap.MapStatus.Anonymous.value)

		return reactiveTemplate.updateFirst(query, update, LocationMap::class.java)
			.map { updateResult -> updateResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	open fun updateSetMapFailed(photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`is`(photoId))

		val update = Update()
			.set(LocationMap.Mongo.Field.MAP_STATUS, LocationMap.MapStatus.Failed.value)

		return reactiveTemplate.updateFirst(query, update, LocationMap::class.java)
			.map { updateResult -> updateResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	open fun increaseAttemptsCountAndNextAttemptTime(photoId: Long, nextAttemptTime: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`is`(photoId))

		val update = Update()
			.inc(LocationMap.Mongo.Field.ATTEMPTS_COUNT, 1)
			.set(LocationMap.Mongo.Field.NEXT_ATTEMPT_TIME, nextAttemptTime)

		return reactiveTemplate.updateFirst(query, update, LocationMap::class.java)
			.map { updateResult -> updateResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	open fun deleteById(photoId: Long, template: ReactiveMongoOperations = reactiveTemplate): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`is`(photoId))

		return template.remove(query, LocationMap::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	open fun deleteAll(photoIds: List<Long>): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`in`(photoIds))
			.limit(photoIds.size)

		return reactiveTemplate.remove(query, LocationMap::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	/**
	 * For test purposes
	 * */

	open fun testFindAll(): Mono<List<LocationMap>> {
		return reactiveTemplate.findAll(LocationMap::class.java)
			.collectList()
	}

	companion object {
		const val COLLECTION_NAME = "location_map"
	}
}