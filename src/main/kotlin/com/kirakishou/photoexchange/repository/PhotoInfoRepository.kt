package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.PhotoInfo
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

open class PhotoInfoRepository(private val template: MongoTemplate,
                               private val mongoSequenceRepo: MongoSequenceRepository) {

    private val logger = LoggerFactory.getLogger(PhotoInfoRepository::class.java)

    private val mongoThreadPoolContext: ThreadPoolDispatcher by lazy {
        newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors(), "mongo")
    }

    suspend fun save(photoInfoParam: PhotoInfo): PhotoInfo {
        return async(mongoThreadPoolContext) {
            val id = mongoSequenceRepo.getNextId()
            photoInfoParam.photoId = id

            try {
                template.save(photoInfoParam)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                return@async PhotoInfo.empty()
            }

            return@async photoInfoParam
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

    suspend fun findPhotoByCandidateUserId(userId: String): PhotoInfo {
        return async(mongoThreadPoolContext) {
            val query = Query().with(Sort(Sort.Direction.ASC, "uploadedOn"))
                    .addCriteria(Criteria.where("whoUploaded").`is`(userId))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))
                    .addCriteria(Criteria.where("candidateFoundOn").ne(0L))
                    .limit(1)

            val result = try {
                template.find(query, PhotoInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                emptyList<PhotoInfo>()
            }

            if (result.isEmpty()) {
                return@async PhotoInfo.empty()
            }

            return@async result.first()
        }.await()
    }

    suspend fun findOldestUploadedPhoto(userId: String): PhotoInfo {
        return async(mongoThreadPoolContext) {
            val query = Query().with(Sort(Sort.Direction.ASC, "uploadedOn"))
                    .addCriteria(Criteria.where("whoUploaded").ne(userId))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))
                    .addCriteria(Criteria.where("candidateFoundOn").`is`(0L))
                    .limit(1)

            val update = Update()
                    .set("candidateFoundOn", TimeUtils.getTimeFast())
                    .set("candidateUserId", userId)

            val result = try {
                template.findAndModify(query, update, PhotoInfo::class.java) ?: PhotoInfo.empty()
            } catch (error: Throwable) {
                logger.error("DB error", error)
                PhotoInfo.empty()
            }

            return@async result
        }.await()
    }

    suspend fun findUploadedPhotosLocations(userId: String, photoNameList: List<String>): List<PhotoInfo> {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("whoUploaded").`is`(userId))
                    .addCriteria(Criteria.where("photoName").`in`(photoNameList))
                    .addCriteria(Criteria.where("candidateFoundOn").gt(0L))

            val result = try {
                template.find(query, PhotoInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                emptyList<PhotoInfo>()
            }

            return@async result
        }.await()
    }

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

    suspend fun updateSetPhotoSuccessfullyDelivered(photoId: Long, userId: String): Boolean {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("photoId").`is`(photoId))
                    .addCriteria(Criteria.where("candidateUserId").`is`(userId))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))

            val update = Update()
                    .set("receivedPhotoBackOn", TimeUtils.getTimeFast())

            val result = try {
                template.updateFirst(query, update, PhotoInfo::class.java).wasAcknowledged()
            } catch (error: Throwable) {
                logger.error("DB error", error)
                false
            }

            return@async result
        }.await()
    }

    suspend fun deleteById(userId: String): Boolean {
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
























