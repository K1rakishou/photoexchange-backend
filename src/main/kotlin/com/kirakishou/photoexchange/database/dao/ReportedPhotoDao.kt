package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.ReportedPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

open class ReportedPhotoDao(
	private val template: ReactiveMongoTemplate
) : BaseDao {
	private val logger = LoggerFactory.getLogger(ReportedPhotoDao::class.java)

	override fun create() {
		if (!template.collectionExists(ReportedPhoto::class.java).block()) {
			template.createCollection(ReportedPhoto::class.java).block()
		}
	}

	override fun clear() {
		if (template.collectionExists(ReportedPhoto::class.java).block()) {
			template.dropCollection(ReportedPhoto::class.java).block()
		}
	}

	fun reportPhoto(reportedPhoto: ReportedPhoto): Mono<Boolean> {
		return template.save(reportedPhoto)
			.map { true }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun unreportPhoto(userId: String, photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return template.remove(query, ReportedPhoto::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun findMany(userId: String, photoIdList: List<Long>): Mono<List<ReportedPhoto>> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`in`(photoIdList))
			.limit(photoIdList.size)

		return template.find(query, ReportedPhoto::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	fun isPhotoReported(userId: String, photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return template.exists(query, ReportedPhoto::class.java)
			.defaultIfEmpty(false)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun deleteByPhotoId(photoId: Long): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))

		return template.remove(query, ReportedPhoto::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun deleteAll(photoIds: List<Long>): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.ID).`in`(photoIds))
			.limit(photoIds.size)

		return template.remove(query, ReportedPhoto::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	companion object {
		const val COLLECTION_NAME = "reported_photo"
	}
}