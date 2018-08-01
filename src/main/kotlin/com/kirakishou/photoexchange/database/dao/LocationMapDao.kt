package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.LocationMap
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import reactor.core.publisher.Mono

class LocationMapDao(
	private val template: ReactiveMongoTemplate
) : BaseDao {
	private val logger = LoggerFactory.getLogger(LocationMapDao::class.java)

	override fun create() {
		if (!template.collectionExists(LocationMap::class.java).block()) {
			template.createCollection(LocationMap::class.java).block()
		}
	}

	override fun clear() {
		if (template.collectionExists(LocationMap::class.java).block()) {
			template.dropCollection(LocationMap::class.java).block()
		}
	}

	fun save(locationMap: LocationMap): Mono<LocationMap> {
		return template.save(locationMap)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(LocationMap.empty())
	}

	fun findOldest(count: Int, currentTime: Long): Mono<List<LocationMap>> {
		val query = Query().with(Sort(Sort.Direction.ASC, LocationMap.Mongo.Field.ID))
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.NEXT_ATTEMPT_TIME).lt(currentTime))
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.MAP_STATUS).`is`(LocationMap.MapStatus.Empty.value))
			.limit(count)

		return template.find(query, LocationMap::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	fun updateSetMapReady(photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`is`(photoId))

		val update = Update()
			.set(LocationMap.Mongo.Field.MAP_STATUS, LocationMap.MapStatus.Ready.value)

		return template.updateFirst(query, update, LocationMap::class.java)
			.map { updateResult -> updateResult.wasAcknowledged() && updateResult.modifiedCount == 1L }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun updateSetMapFailed(photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`is`(photoId))

		val update = Update()
			.set(LocationMap.Mongo.Field.MAP_STATUS, LocationMap.MapStatus.Ready.value)

		return template.updateFirst(query, update, LocationMap::class.java)
			.map { updateResult -> updateResult.wasAcknowledged() && updateResult.modifiedCount == 1L }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun increaseAttemptsCountAndNextAttemptTime(photoId: Long, nextAttemptTime: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`is`(photoId))

		val update = Update()
			.inc(LocationMap.Mongo.Field.ATTEMPTS_COUNT, 1)
			.set(LocationMap.Mongo.Field.NEXT_ATTEMPT_TIME, nextAttemptTime)

		return template.updateFirst(query, update, LocationMap::class.java)
			.map { updateResult -> updateResult.wasAcknowledged() && updateResult.modifiedCount == 1L }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun deleteById(photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`is`(photoId))

		return template.remove(query, LocationMap::class.java)
			.map { deletionResult -> deletionResult.deletedCount == 1L && deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun deleteAll(photoIds: List<Long>): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(LocationMap.Mongo.Field.PHOTO_ID).`in`(photoIds))
			.limit(photoIds.size)

		return template.remove(query, LocationMap::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() && deletionResult.deletedCount.toInt() == photoIds.size }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	companion object {
		const val COLLECTION_NAME = "location_map"
	}
}