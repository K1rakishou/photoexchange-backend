package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.database.entity.ReportedPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

open class ReportedPhotoDao(
	template: ReactiveMongoTemplate
) : BaseDao(template) {
	private val logger = LoggerFactory.getLogger(ReportedPhotoDao::class.java)

	override fun create() {
		createCollectionIfNotExists(COLLECTION_NAME)
	}

	override fun clear() {
		dropCollectionIfExists(COLLECTION_NAME)
	}

	open fun reportPhoto(reportedPhoto: ReportedPhoto): Mono<Boolean> {
		return reactiveTemplate.save(reportedPhoto)
			.map { true }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	open fun unreportPhoto(userId: String, photoName: String): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_NAME).`is`(photoName))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return reactiveTemplate.remove(query, ReportedPhoto::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	open fun findManyReportsByPhotoNameList(userId: String, photoNameList: List<String>): Mono<List<ReportedPhoto>> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_NAME).`in`(photoNameList))
			.limit(photoNameList.size)

		return reactiveTemplate.find(query, ReportedPhoto::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	open fun isPhotoReported(userId: String, photoName: String): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_NAME).`is`(photoName))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return reactiveTemplate.exists(query, ReportedPhoto::class.java)
			.defaultIfEmpty(false)
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	open fun deleteReportByPhotoName(photoName: String, template: ReactiveMongoOperations = reactiveTemplate): Mono<Boolean> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_NAME).`is`(photoName))

		return template.remove(query, ReportedPhoto::class.java)
			.map { deletionResult -> deletionResult.wasAcknowledged() }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	/**
	 * For test purposes
	 * */

	open fun testFindAll(): Mono<List<ReportedPhoto>> {
		return reactiveTemplate.findAll(ReportedPhoto::class.java)
			.collectList()
	}

	companion object {
		const val COLLECTION_NAME = "reported_photo"
	}
}