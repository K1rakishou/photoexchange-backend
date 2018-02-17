package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.repo.PhotoInfo
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

open class PhotoInfoRepository(
    private val template: MongoTemplate,
    private val mongoSequenceRepo: MongoSequenceRepository,
    private val mongoThreadPoolContext: ThreadPoolDispatcher
) {
    private val logger = LoggerFactory.getLogger(PhotoInfoRepository::class.java)
    private val mutex = Mutex()

    suspend fun save(photoInfo: PhotoInfo): PhotoInfo {
        return async(mongoThreadPoolContext) {
            return@async mutex.withLock {
                val id = mongoSequenceRepo.getNextPhotoId()
                photoInfo.photoId = id

                try {
                    template.save(photoInfo)
                } catch (error: Throwable) {
                    logger.error("DB error", error)
                    return@withLock PhotoInfo.empty()
                }

                return@withLock photoInfo
            }
        }.await()
    }

    suspend fun countUserUploadedPhotos(userId: String): Long {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("whoUploaded").`is`(userId))

            val count = try {
                template.count(query, PhotoInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                -1L
            }

            return@async count
        }.await()
    }

    suspend fun countUserReceivedBackPhotos(userId: String): Long {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("candidateUserId").`is`(userId))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").gt(0))
                    .addCriteria(Criteria.where("candidateFoundOn").gt(0))

            val count = try {
                template.count(query, PhotoInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                -1L
            }

            return@async count
        }.await()
    }

    suspend fun find(userId: String, photoName: String): PhotoInfo {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("whoUploaded").`is`(userId))
                    .addCriteria(Criteria.where("photoName").`is`(photoName))

            val photoInfo = try {
                template.findOne(query, PhotoInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                PhotoInfo.empty()
            }

            if (photoInfo == null) {
                return@async PhotoInfo.empty()
            }

            return@async photoInfo
        }.await()
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

//    suspend fun findOldestUploadedPhoto(userId: String): PhotoInfo {
//        return async(mongoThreadPoolContext) {
//            val query = Query().with(Sort(Sort.Direction.ASC, "uploadedOn"))
//                    .addCriteria(Criteria.where("whoUploaded").ne(userId))
//                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))
//                    .addCriteria(Criteria.where("candidateFoundOn").`is`(0L))
//                    .limit(1)
//
//            val update = Update()
//                    .set("candidateFoundOn", TimeUtils.getTimeFast())
//                    .set("candidateUserId", userId)
//
//            val result = try {
//                template.findAndModify(query, update, PhotoInfo::class.java) ?: PhotoInfo.empty()
//            } catch (error: Throwable) {
//                logger.error("DB error", error)
//                PhotoInfo.empty()
//            }
//
//            return@async result
//        }.await()
//    }

    suspend fun findOldestUploadedPhoto(uploaderUserId: String, uploadingPhotoName: String): PhotoInfo {
        return async(mongoThreadPoolContext) {
            val query = Query().with(Sort(Sort.Direction.ASC, "uploadedOn"))
                    .addCriteria(Criteria.where("whoUploaded").ne(uploaderUserId))
                    .addCriteria(Criteria.where("photoName").ne(uploadingPhotoName))
                    .addCriteria(Criteria.where("whoReceived").`is`(""))
                    .addCriteria(Criteria.where("receivedOn").`is`(0L))

            val result = try {
                template.findOne(query, PhotoInfo::class.java) ?: PhotoInfo.empty()
            } catch (error: Throwable) {
                logger.error("DB error", error)
                PhotoInfo.empty()
            }

            return@async result
        }.await()
    }

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

    suspend fun cleanCandidatesFromPhotosOverTime(time: Long) {
        async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("candidateFoundOn").lt(time))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))

            val update = Update()
                    .set("candidateFoundOn", 0L)
                    .set("candidateUserId", "")

            try {
                template.updateMulti(query, update, PhotoInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
            }
        }.await()
    }

    suspend fun findOlderThan(time: Long): List<PhotoInfo> {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("uploadedOn").lt(time))

            val result = try {
                template.find(query, PhotoInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                emptyList<PhotoInfo>()
            }

            return@async result
        }.await()
    }

    suspend fun updateSetPhotoSuccessfullyDelivered(photoId: Long, userId: String, time: Long): Boolean {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("photoId").`is`(photoId))
                    .addCriteria(Criteria.where("candidateUserId").`is`(userId))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))

            val update = Update()
                    .set("receivedPhotoBackOn", time)

            val result = try {
                template.updateFirst(query, update, PhotoInfo::class.java).wasAcknowledged()
            } catch (error: Throwable) {
                logger.error("DB error", error)
                false
            }

            return@async result
        }.await()
    }

    suspend fun updateSetPhotoReceiver(userId: String, photoName: String, receiverId: String, time: Long): Boolean {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("whoUploaded").`is`(userId))
                    .addCriteria(Criteria.where("photoName").`is`(photoName))

            val update = Update()
                    .set("whoReceived", receiverId)
                    .set("receivedOn", time)

            val result = try {
                template.updateFirst(query, update, PhotoInfo::class.java).wasAcknowledged()
            } catch (error: Throwable) {
                logger.error("DB error", error)
                false
            }

            return@async result
        }.await()
    }

    suspend fun deleteUserById(userId: String): Boolean {
        return async(mongoThreadPoolContext) {
            val result = try {
                template.remove(Query.query(Criteria.where("whoUploaded").`is`(userId))).wasAcknowledged()
            } catch (error: Throwable) {
                logger.error("DB error", error)
                false
            }

            return@async result
        }.await()
    }

    suspend fun deleteAll(ids: List<Long>): Boolean {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("photoId").`in`(ids))

            val result = try {
                template.remove(query, PhotoInfo::class.java).wasAcknowledged()
            } catch (error: Throwable) {
                logger.error("DB error", error)
                false
            }

            return@async result
        }.await()
    }

    companion object {
        const val COLLECTION_NAME = "photo_info"
    }
}
























