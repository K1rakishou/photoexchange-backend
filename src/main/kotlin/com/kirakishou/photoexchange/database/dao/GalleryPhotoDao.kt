package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.GalleryPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

class GalleryPhotoDao(
	private val template: MongoTemplate
) : BaseDao {
	private val logger = LoggerFactory.getLogger(GalleryPhotoDao::class.java)

	override fun create() {
		if (!template.collectionExists(GalleryPhoto::class.java)) {
			template.createCollection(GalleryPhoto::class.java)
		}
	}

	override fun clear() {
		if (template.collectionExists(GalleryPhoto::class.java)) {
			template.dropCollection(GalleryPhoto::class.java)
		}
	}

	suspend fun save(galleryPhoto: GalleryPhoto): Boolean {
		try {
			template.save(galleryPhoto)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return false
		}

		return true
	}

	suspend fun findPaged(lastId: Long, count: Int): List<GalleryPhoto> {
		val query = Query().with(Sort(Sort.Direction.DESC, GalleryPhoto.Mongo.Field.ID))
			.addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.ID).lt(lastId))
			.limit(count)

		val result = try {
			template.find(query, GalleryPhoto::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			emptyList<GalleryPhoto>()
		}

		return result
	}

	fun findManyByIdList(photoIds: List<Long>): List<GalleryPhoto> {
		val query = Query()
			.addCriteria((Criteria.where(GalleryPhoto.Mongo.Field.ID).`in`(photoIds)))
			.limit(photoIds.size)

		val result = try {
			template.find(query, GalleryPhoto::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			emptyList<GalleryPhoto>()
		}

		return result
	}

	companion object {
		const val COLLECTION_NAME = "gallery_photo"
	}
}