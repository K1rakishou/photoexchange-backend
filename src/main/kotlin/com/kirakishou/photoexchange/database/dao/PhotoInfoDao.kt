package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.PhotoInfo
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

open class PhotoInfoDao(
	private val template: MongoTemplate
) {
	private val logger = LoggerFactory.getLogger(PhotoInfoDao::class.java)

	fun init() {
		if (!template.collectionExists(PhotoInfo::class.java)) {
			template.createCollection(PhotoInfo::class.java)
		}
	}

	suspend fun save(photoInfo: PhotoInfo): PhotoInfo {
		try {
			template.save(photoInfo)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return PhotoInfo.empty()
		}

		return photoInfo
	}

	suspend fun findAllPhotoIdsByUserId(userId: String): List<Long> {
		return findAllPhotosByUserId(userId).map { it.photoId }
	}

	suspend fun findAllPhotosByUserId(userId: String): List<PhotoInfo> {
		val getUploadedPhotosQuery = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))

		val uploadedPhotos = try {
			template.find(getUploadedPhotosQuery, PhotoInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			emptyList<PhotoInfo>()
		}

		return uploadedPhotos
	}

	suspend fun countAllUserUploadedPhotos(userId: String): Long {
		val getUploadedPhotosQuery = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))

		val result = try {
			template.count(getUploadedPhotosQuery, PhotoInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			0L
		}

		return result
	}

	suspend fun find(photoId: Long): PhotoInfo {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

		val photoInfo = try {
			template.findOne(query, PhotoInfo::class.java) ?: PhotoInfo.empty()
		} catch (error: Throwable) {
			logger.error("DB error", error)
			PhotoInfo.empty()
		}

		return photoInfo
	}

	suspend fun find(userId: String, photoName: String): PhotoInfo {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(photoName))

		val photoInfo = try {
			template.findOne(query, PhotoInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			PhotoInfo.empty()
		}

		if (photoInfo == null) {
			return PhotoInfo.empty()
		}

		return photoInfo
	}

	suspend fun findByExchangeIdAndUserId(userId: String, exchangeId: Long): PhotoInfo {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGE_ID).`is`(exchangeId))

		val photoInfo = try {
			template.findOne(query, PhotoInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			PhotoInfo.empty()
		}

		if (photoInfo == null) {
			return PhotoInfo.empty()
		}

		return photoInfo
	}

	suspend fun findMany(userId: String, photoNames: List<String>): List<PhotoInfo> {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`in`(photoNames))

		val photoInfo = try {
			template.find(query, PhotoInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			emptyList<PhotoInfo>()
		}

		if (photoInfo == null) {
			return emptyList()
		}

		return photoInfo
	}

	suspend fun findMany(photoIdList: List<Long>): List<PhotoInfo> {
		val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.PHOTO_ID))
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`in`(photoIdList))
			.limit(photoIdList.size)

		val photoInfoList = try {
			template.find(query, PhotoInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			emptyList<PhotoInfo>()
		}

		if (photoInfoList == null) {
			return emptyList()
		}

		return photoInfoList
	}

	suspend fun findOlderThan(time: Long): List<PhotoInfo> {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).lt(time))

		val result = try {
			template.find(query, PhotoInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			emptyList<PhotoInfo>()
		}

		return result
	}

	suspend fun updateSetExchangeId(photoId: Long, exchangeId: Long): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

		val update = Update()
			.set(PhotoInfo.Mongo.Field.EXCHANGE_ID, exchangeId)

		val result = try {
			val updateResult = template.updateFirst(query, update, PhotoInfo::class.java)
			updateResult.wasAcknowledged() && updateResult.modifiedCount == 1L
		} catch (error: Throwable) {
			logger.error("DB error", error)
			false
		}

		return result
	}

	suspend fun deleteById(photoId: Long) {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

		try {
			template.remove(query, PhotoInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
		}
	}

	suspend fun deleteUserById(userId: String): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).`is`(userId))
			.limit(1)

		val result = try {
			val deleteResult = template.remove(query, PhotoInfo::class.java)
			deleteResult.wasAcknowledged() && deleteResult.deletedCount == 1L
		} catch (error: Throwable) {
			logger.error("DB error", error)
			false
		}

		return result
	}

	suspend fun deleteAll(ids: List<Long>): Boolean {
		val count = ids.size

		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`in`(ids))
			.limit(count)

		val result = try {
			val deleteResult = template.remove(query, PhotoInfo::class.java)
			deleteResult.wasAcknowledged() && deleteResult.deletedCount == count.toLong()
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return false
		}

		return result
	}

	suspend fun photoNameExists(generatedName: String): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(generatedName))

		return template.exists(query, PhotoInfo::class.java)
	}

	suspend fun getPhotoIdByName(photoName: String): Long {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(photoName))

		val result = try {
			template.findOne(query, PhotoInfo::class.java)?.photoId ?: -1L
		} catch (error: Throwable) {
			logger.error("DB error", error)
			return -1L
		}

		return result
	}

	suspend fun incrementPhotoFavouritesCount(photoName: String): Boolean {
		val query = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(photoName))
			.limit(1)

		val update = Update()
			.inc(PhotoInfo.Mongo.Field.FAVOURITES_COUNT, 1)

		val result = try {
			val updateResult = template.updateFirst(query, update, PhotoInfo::class.java)
			updateResult.wasAcknowledged() && updateResult.modifiedCount == 1L
		} catch (error: Throwable) {
			logger.error("DB error", error)
			false
		}

		return result
	}

	companion object {
		const val COLLECTION_NAME = "photo_info"
	}
}
























