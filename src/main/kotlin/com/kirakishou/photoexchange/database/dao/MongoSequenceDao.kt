package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.MongoSequence
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import reactor.core.publisher.Mono

open class MongoSequenceDao(
	template: ReactiveMongoTemplate
) : BaseDao(template) {
	private val PHOTO_INFO_SEQUENCE_NAME = "photo_info_sequence"
	private val GALLERY_PHOTO_SEQUENCE_NAME = "gallery_photo_sequence"
	private val FAVOURITED_PHOTO_SEQUENCE_NAME = "favourited_photo_sequence"
	private val REPORTED_PHOTO_SEQUENCE_NAME = "reported_photo_sequence"
	private val USER_INFO_SEQUENCE_NAME = "user_info_sequence"
	private val LOCATION_MAP_SEQUENCE_NAME = "location_map_sequence"

	override fun create() {
		createCollectionIfNotExists(COLLECTION_NAME)
	}

	override fun clear() {
		dropCollectionIfExists(COLLECTION_NAME)
	}

	private fun getNextId(sequenceName: String): Mono<Long> {
		val query = Query()
			.addCriteria(Criteria.where(MongoSequence.Mongo.Field.SEQUENCE_NAME).`is`(sequenceName))

		val update = Update()
			.inc(MongoSequence.Mongo.Field.SEQUENCE_ID, 1)

		val options = FindAndModifyOptions.options().returnNew(true).upsert(true)

		return template.findAndModify(query, update, options, MongoSequence::class.java)
			.map { it.id }
	}

	fun getNextPhotoId(): Mono<Long> {
		return getNextId(PHOTO_INFO_SEQUENCE_NAME)
	}

	fun getNextGalleryPhotoId(): Mono<Long> {
		return getNextId(GALLERY_PHOTO_SEQUENCE_NAME)
	}

	fun getNextFavouritedPhotoId(): Mono<Long> {
		return getNextId(FAVOURITED_PHOTO_SEQUENCE_NAME)
	}

	fun getNextReportedPhotoId(): Mono<Long> {
		return getNextId(REPORTED_PHOTO_SEQUENCE_NAME)
	}

	fun getNextUserId(): Mono<Long> {
		return getNextId(USER_INFO_SEQUENCE_NAME)
	}

	fun getNextLocationMapId(): Mono<Long> {
		return getNextId(LOCATION_MAP_SEQUENCE_NAME)
	}

	companion object {
	  const val COLLECTION_NAME = "mongo_sequence"
	}
}