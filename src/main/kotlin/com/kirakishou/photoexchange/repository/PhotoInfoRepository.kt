package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.PhotoInfo
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

open class PhotoInfoRepository(private val template: MongoTemplate,
                               private val mongoSequenceRepo: MongoSequenceRepository) {

    private val mongoThreadPoolContext: ThreadPoolDispatcher by lazy {
        newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors(), "mongo")
    }

    suspend fun save(photoInfoParam: PhotoInfo): PhotoInfo {
        return async(mongoThreadPoolContext) {
            val id = mongoSequenceRepo.getNextId(SEQUENCE_NAME)
            photoInfoParam.photoId = id
            template.save(photoInfoParam)

            return@async photoInfoParam
        }.await()
    }

    suspend fun countUserUploadedPhotos(userId: String): Long {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("whoUploaded").`is`(userId))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))
                    .addCriteria(Criteria.where("candidateFoundOn").`is`(0L))

            return@async template.count(query, PhotoInfo::class.java)
        }.await()
    }

    suspend fun countUserReceivedBackPhotos(userId: String): Long {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("candidateUserId").`is`(userId))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").gt(0))
                    .addCriteria(Criteria.where("candidateFoundOn").gt(0))

            return@async template.count(query, PhotoInfo::class.java)
        }.await()
    }

    suspend fun findPhotoInfo(userId: String): PhotoInfo {
        return async(mongoThreadPoolContext) {
            val query = Query().with(Sort(Sort.Direction.ASC, "uploadedOn"))
                    .addCriteria(Criteria.where("whoUploaded").ne(userId))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))
                    .addCriteria(Criteria.where("candidateFoundOn").`is`(0L))
                    .limit(1)

            val update = Update()
                    .set("candidateFoundOn", TimeUtils.getTime())
                    .set("candidateUserId", userId)

            return@async template.findAndModify(query, update, PhotoInfo::class.java)
        }.await()
    }

    suspend fun updateSetPhotoSuccessfullyDelivered(photoId: Long, userId: String): Boolean {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("photoId").`is`(photoId))
                    .addCriteria(Criteria.where("candidateUserId").`is`(userId))
                    .addCriteria(Criteria.where("receivedPhotoBackOn").`is`(0L))

            val update = Update()
                    .set("receivedPhotoBackOn", TimeUtils.getTime())

            return@async template.updateFirst(query, update, PhotoInfo::class.java).wasAcknowledged()
        }.await()
    }

    suspend fun deleteById(userId: String): Boolean {
        return async(mongoThreadPoolContext) {
            return@async template.remove(Query.query(Criteria.where("whoUploaded").`is`(userId))).wasAcknowledged()
        }.await()
    }

    companion object {
        const val COLLECTION_NAME = "photo_info"
        const val SEQUENCE_NAME = "photo_info_sequence"
    }
}
























