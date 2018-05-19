package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.ReportedPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

class ReportedPhotoDao(
	private val template: MongoTemplate
) : BaseDao {
	private val logger = LoggerFactory.getLogger(ReportedPhotoDao::class.java)

	override fun create() {
		if (!template.collectionExists(ReportedPhoto::class.java)) {
			template.createCollection(ReportedPhoto::class.java)
		}
	}

	override fun clear() {
		if (template.collectionExists(ReportedPhoto::class.java)) {
			template.dropCollection(ReportedPhoto::class.java)
		}
	}

	suspend fun reportPhoto(reportedPhoto: ReportedPhoto): Boolean {
		try {
			template.save(reportedPhoto)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return false
		}

		return true
	}

	suspend fun unreportPhoto(userId: String, photoId: Long): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))

		val result = try {
			val deletionResult = template.remove(query, ReportedPhoto::class.java)
			deletionResult.deletedCount == 1L && deletionResult.wasAcknowledged()
		} catch (error: Throwable) {
			logger.error("DB error", error)
			false
		}

		return result
	}

	suspend fun findMany(userId: String, photoIdList: List<Long>): List<ReportedPhoto> {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`in`(photoIdList))
			.limit(photoIdList.size)

		val result = try {
			template.find(query, ReportedPhoto::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			emptyList<ReportedPhoto>()
		}

		return result
	}

	suspend fun isPhotoReported(userId: String, photoId: Long): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return template.exists(query, ReportedPhoto::class.java)
	}

	suspend fun deleteByPhotoId(photoId: Long) {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))

		template.remove(query, ReportedPhoto::class.java)
	}

	suspend fun deleteAll(photoIds: List<Long>): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.ID).`in`(photoIds))
			.limit(photoIds.size)

		return try {
			val deletionResult = template.remove(query, ReportedPhoto::class.java)
			deletionResult.wasAcknowledged() && deletionResult.deletedCount.toInt() == photoIds.size
		} catch (error: Throwable) {
			logger.error("DB error", error)
			false
		}
	}

	suspend fun countByPhotoId(photoId: Long): Long {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))

		return try {
			template.count(query, ReportedPhoto::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			-1L
		}
	}

	suspend fun countByUserId(userId: Long): Long {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return try {
			template.count(query, ReportedPhoto::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			-1L
		}
	}

	companion object {
		const val COLLECTION_NAME = "reported_photo"
	}
}