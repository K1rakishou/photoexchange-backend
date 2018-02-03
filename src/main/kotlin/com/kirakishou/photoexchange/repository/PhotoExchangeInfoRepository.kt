package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.repo.PhotoExchangeInfo
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

class PhotoExchangeInfoRepository(
    private val template: MongoTemplate,
    private val mongoSequenceRepo: MongoSequenceRepository,
    private val mongoThreadPoolContext: ThreadPoolDispatcher
) {
    private val logger = LoggerFactory.getLogger(PhotoExchangeInfoRepository::class.java)
    private val mutex = Mutex()

    suspend fun findVacantPhotoExchangeInfo(): PhotoExchangeInfo {
        return async(mongoThreadPoolContext) {
            val query = Query().with(Sort(Sort.Direction.ASC, "createdOn"))
                    .addCriteria(Criteria.where("receiverPhotoId").`is`(-1L))

            val photoExchangeInfo = try {
                template.findOne(query, PhotoExchangeInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                PhotoExchangeInfo.empty()
            }

            if (photoExchangeInfo == null) {
                return@async PhotoExchangeInfo.empty()
            }

            return@async photoExchangeInfo
        }.await()
    }

    suspend fun createNew(photoExchangeInfo: PhotoExchangeInfo): PhotoExchangeInfo {
        return async(mongoThreadPoolContext) {
            return@async mutex.withLock {
                val id = mongoSequenceRepo.getNextPhotoExchangeId()
                photoExchangeInfo.photoExchangeInfoId = id

                try {
                    template.save(photoExchangeInfo)
                } catch (error: Throwable) {
                    logger.error("DB error", error)
                    return@withLock PhotoExchangeInfo.empty()
                }

                return@withLock photoExchangeInfo
            }
        }.await()
    }

    suspend fun exists(id: Long): Boolean {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("uploaderPhotoId").`is`(id))

            try {
                return@async template.exists(query, PhotoExchangeInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                return@async true
            }

        }.await()
    }

    suspend fun findByUploaderPhotoId(uploaderPhotoId: Long): PhotoExchangeInfo {
        return async(mongoThreadPoolContext) {
            val query = Query()
                    .addCriteria(Criteria.where("uploaderPhotoId").`is`(uploaderPhotoId))

            val photoExchangeInfo = try {
                template.findOne(query, PhotoExchangeInfo::class.java)
            } catch (error: Throwable) {
                logger.error("DB error", error)
                return@async PhotoExchangeInfo.empty()
            }

            if (photoExchangeInfo == null) {
                return@async PhotoExchangeInfo.empty()
            }

            return@async photoExchangeInfo
        }.await()
    }

    companion object {
        const val COLLECTION_NAME = "photo_exchange_info"
    }
}