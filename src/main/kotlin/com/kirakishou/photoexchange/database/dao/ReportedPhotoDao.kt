package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.ReportedPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

class ReportedPhotoDao(
	private val template: MongoTemplate
) {
	private val logger = LoggerFactory.getLogger(ReportedPhotoDao::class.java)

	fun init() {
		if (!template.collectionExists(ReportedPhoto::class.java)) {
			template.createCollection(ReportedPhoto::class.java)
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

	suspend fun isPhotoReported(userId: String, photoId: Long): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))
			.addCriteria(Criteria.where(ReportedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return template.exists(query, ReportedPhoto::class.java)
	}

	companion object {
		const val COLLECTION_NAME = "reported_photo"
	}
}