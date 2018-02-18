package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.PhotoInfo
import org.slf4j.LoggerFactory
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

	suspend fun findAllUserUploadedPhotoInfoIds(userId: String): List<Long> {
		val getUploadedPhotosQuery = Query()
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.WHO_UPLOADED).`is`(userId))

		val uploadedPhotos = try {
			template.find(getUploadedPhotosQuery, PhotoInfo::class.java)
		} catch (error: Throwable) {
			logger.error("DB error", error)
			emptyList<PhotoInfo>()
		}

		return uploadedPhotos.map { it.photoId }
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
			.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.WHO_UPLOADED).`is`(userId))
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

//    suspend fun findById(photoId: Long): PhotoInfo {
//        return async(mongoThreadPoolContext) {
//            val query = Query()
//                    .addCriteria(Criteria.where("photoId").`is`(photoId))
//
//            val photoInfo = try {
//                template.findOne(query, PhotoInfo::class.java)
//            } catch (error: Throwable) {
//                logger.error("DB error", error)
//                PhotoInfo.empty()
//            }
//
//            if (photoInfo == null) {
//                return@async PhotoInfo.empty()
//            }
//
//            return@async photoInfo
//        }.await()
//    }
//
//    suspend fun findPhotoByCandidateUserIdList(userIdList: List<String>): List<PhotoInfo> {
//        return async(mongoThreadPoolContext) {
//            val query = Query().with(Sort(Sort.Direction.ASC, "uploadedOn"))
//                    .addCriteria(Criteria.where("whoUploaded").`in`(userIdList))
//                    .addCriteria(Criteria.where("receivedPhotoBackOn").gt(0L))
//                    .addCriteria(Criteria.where("candidateFoundOn").gt(0L))
//                    .limit(1)
//
//            val result = try {
//                template.find(query, PhotoInfo::class.java)
//            } catch (error: Throwable) {
//                logger.error("DB error", error)
//                emptyList<PhotoInfo>()
//            }
//
//            return@async result
//        }.await()
//    }

	//TODO: remake
//    suspend fun findOldestUploadedPhoto(uploaderUserId: String, uploadingPhotoName: String): PhotoInfo {
//        return async(mongoThreadPoolContext) {
//            val query = Query().with(Sort(Sort.Direction.ASC, "uploadedOn"))
//                    .addCriteria(Criteria.where("whoUploaded").ne(uploaderUserId))
//                    .addCriteria(Criteria.where("photoName").ne(uploadingPhotoName))
//                    .addCriteria(Criteria.where("whoReceived").`is`(""))
//                    .addCriteria(Criteria.where("receivedOn").`is`(0L))
//
//            val result = try {
//                template.findOne(query, PhotoInfo::class.java) ?: PhotoInfo.empty()
//            } catch (error: Throwable) {
//                logger.error("DB error", error)
//                PhotoInfo.empty()
//            }
//
//            return@async result
//        }.await()
//    }

//    suspend fun findUploadedPhotosLocations(userId: String, photoNameList: List<String>): List<PhotoInfo> {
//        return async(mongoThreadPoolContext) {
//            val query = Query()
//                    .addCriteria(Criteria.where("whoUploaded").`is`(userId))
//                    .addCriteria(Criteria.where("photoName").`in`(photoNameList))
//                    .addCriteria(Criteria.where("candidateFoundOn").gt(0L))
//
//            val result = try {
//                template.find(query, PhotoInfo::class.java)
//            } catch (error: Throwable) {
//                logger.error("DB error", error)
//                emptyList<PhotoInfo>()
//            }
//
//            return@async result
//        }.await()
//    }

//    suspend fun cleanCandidatesFromPhotosOverTime(time: Long) {
//        async(mongoThreadPoolContext) {
//            val query = Query()
//                    .addCriteria(Criteria.where("candidateFoundOn").lt(time))
//                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))
//
//            val update = Update()
//                    .set("candidateFoundOn", 0L)
//                    .set("candidateUserId", "")
//
//            try {
//                template.updateMulti(query, update, PhotoInfo::class.java)
//            } catch (error: Throwable) {
//                logger.error("DB error", error)
//            }
//        }.await()
//    }

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

	fun updateSetExchangeId(photoId: Long, exchangeId: Long): Boolean {
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

	//TODO: remake
	suspend fun updateSetPhotoSuccessfullyDelivered(photoId: Long, userId: String, time: Long): Boolean {
		val query = Query()
			.addCriteria(Criteria.where("photoId").`is`(photoId))
			.addCriteria(Criteria.where("candidateUserId").`is`(userId))
			.addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))
			.limit(1)

		val update = Update()
			.set("receivedPhotoBackOn", time)

		val result = try {
			val updateResult = template.updateFirst(query, update, PhotoInfo::class.java)
			updateResult.wasAcknowledged() && updateResult.modifiedCount == 1L
		} catch (error: Throwable) {
			logger.error("DB error", error)
			false
		}

		return result
	}

	//TODO: remake
//    suspend fun updateSetPhotoReceiver(userId: String, photoName: String, receiverId: String, time: Long): Boolean {
//        return async(mongoThreadPoolContext) {
//            val query = Query()
//                    .addCriteria(Criteria.where("whoUploaded").`is`(userId))
//                    .addCriteria(Criteria.where("photoName").`is`(photoName))
//
//            val update = Update()
//                    .set("whoReceived", receiverId)
//                    .set("receivedOn", time)
//
//            val result = try {
//                template.updateFirst(query, update, PhotoInfo::class.java).wasAcknowledged()
//            } catch (error: Throwable) {
//                logger.error("DB error", error)
//                false
//            }
//
//            return@async result
//        }.await()
//    }

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

	companion object {
		const val COLLECTION_NAME = "photo_info"
	}
}
























