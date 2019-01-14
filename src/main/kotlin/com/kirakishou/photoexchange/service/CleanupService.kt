package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.util.TimeUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

open class CleanupService(
  private val photosRepository: PhotosRepository,
  private val cleanupInterval: Long,
  private val uploadedEarlierThanTimeDelta: Long,
  private val deletedEarlierThanTimeDelta: Long
) {
  private val logger = LoggerFactory.getLogger(CleanupService::class.java)
  private val lastTimeCheck = AtomicLong(System.currentTimeMillis())
  private val inProgress = AtomicBoolean(false)
  private val photosPerQuery = 100

  open suspend fun tryToStartCleaningRoutine() {
    val now = System.currentTimeMillis()
    val oldTime = lastTimeCheck.get()

    logger.debug("time delta = ${now - lastTimeCheck.get()}, cleanupInterval = $cleanupInterval")
    val timeHasCome = (now - lastTimeCheck.get()) > cleanupInterval

    if (timeHasCome && lastTimeCheck.compareAndSet(oldTime, now)) {
      startCleaningRoutine()
    }
  }

  open suspend fun startCleaningRoutine() {
    if (inProgress.getAndSet(true)) {
      logger.debug("Already in progress")
      return
    }

    try {
      val now = TimeUtils.getCurrentDateTime()
      logger.debug("Start cleanDatabaseAndPhotos routine")

      val markedAsDeletedCount = photosRepository.markAsDeletedPhotosUploadedEarlierThan(
        now.minus(uploadedEarlierThanTimeDelta),
        now,
        photosPerQuery
      )

      if (markedAsDeletedCount == -1) {
        logger.debug("Could not mark photos as deleted")
        return
      }

      logger.debug("Marked as deleted $markedAsDeletedCount photos")

      photosRepository.cleanDatabaseAndPhotos(
        now.minus(deletedEarlierThanTimeDelta),
        photosPerQuery
      )
    } catch (error: Throwable) {
      logger.error("Error while cleaning up", error)
    } finally {
      logger.debug("End cleanDatabaseAndPhotos routine")
      inProgress.set(false)
    }
  }

}