package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.FavouritedPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

class FavouritedPhotoDao(
	private val template: MongoTemplate
) {

	private val logger = LoggerFactory.getLogger(FavouritedPhotoDao::class.java)

	fun init() {
		if (!template.collectionExists(FavouritedPhoto::class.java)) {
			template.createCollection(FavouritedPhoto::class.java)
		}
	}

	suspend fun favouritePhoto(favouritedPhoto: FavouritedPhoto): Boolean {
		try {
			template.save(favouritedPhoto)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return false
		}

		return true
	}

	fun isPhotoFavourited(userId: String, photoId: Long): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.PHOTO_ID).`is`(photoId))
			.addCriteria(Criteria.where(FavouritedPhoto.Mongo.Field.USER_ID).`is`(userId))

		return template.exists(query, FavouritedPhoto::class.java)
	}

	companion object {
		const val COLLECTION_NAME = "favourited_photo"
	}
}