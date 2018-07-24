package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.GalleryPhoto
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Mono

open class GalleryPhotoDao(
	private val template: ReactiveMongoTemplate
) : BaseDao {
	private val logger = LoggerFactory.getLogger(GalleryPhotoDao::class.java)

	override fun create() {
		if (!template.collectionExists(GalleryPhoto::class.java).block()) {
			template.createCollection(GalleryPhoto::class.java).block()
		}
	}

	override fun clear() {
		if (template.collectionExists(GalleryPhoto::class.java).block()) {
			template.dropCollection(GalleryPhoto::class.java).block()
		}
	}

	fun save(galleryPhoto: GalleryPhoto): Mono<Boolean> {
		return template.save(galleryPhoto)
			.map { true }
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(false)
	}

	fun findPaged(lastId: Long, count: Int): Mono<List<GalleryPhoto>> {
		val query = Query().with(Sort(Sort.Direction.DESC, GalleryPhoto.Mongo.Field.ID))
			.addCriteria(Criteria.where(GalleryPhoto.Mongo.Field.ID).lt(lastId))
			.limit(count)

		return template.find(query, GalleryPhoto::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	fun findManyByIdList(photoIds: List<Long>): Mono<List<GalleryPhoto>> {
		val query = Query().with(Sort(Sort.Direction.DESC, GalleryPhoto.Mongo.Field.ID))
			.addCriteria((Criteria.where(GalleryPhoto.Mongo.Field.ID).`in`(photoIds)))
			.limit(photoIds.size)

		return template.find(query, GalleryPhoto::class.java)
			.collectList()
			.defaultIfEmpty(emptyList())
			.doOnError { error -> logger.error("DB error", error) }
			.onErrorReturn(emptyList())
	}

	companion object {
		const val COLLECTION_NAME = "gallery_photo"
	}
}