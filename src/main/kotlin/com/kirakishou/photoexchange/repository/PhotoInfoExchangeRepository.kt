package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

open class PhotoInfoExchangeRepository(
    private val template: MongoTemplate,
    private val mongoSequenceRepo: MongoSequenceRepository,
    private val mongoThreadPoolContext: ThreadPoolDispatcher
) {
    private val logger = LoggerFactory.getLogger(PhotoInfoExchangeRepository::class.java)
    private val mutex = Mutex()

    suspend fun save(photoInfoExchange: PhotoInfoExchange): PhotoInfoExchange {
        return async(mongoThreadPoolContext) {
            return@async mutex.withLock {
                val id = mongoSequenceRepo.getNextPhotoExchangeId()
                photoInfoExchange.exchangeId = id

                try {
                    template.save(photoInfoExchange)
                } catch (error: Throwable) {
                    logger.error("DB error", error)
                    return@withLock PhotoInfoExchange.empty()
                }

                return@withLock photoInfoExchange
            }
        }.await()
    }

    companion object {
        const val COLLECTION_NAME = "photo_info_exchange"
    }
}