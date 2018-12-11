package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.util.TimeUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

open class CleanupService(
  private val photoInfoRepository: PhotoInfoRepository,
  private val cleanupInterval: Long,
  private val uploadedEarlierThanTimeDelta: Long,
  private val deletedEarlierThanTimeDelta: Long
) {
  private val logger = LoggerFactory.getLogger(CleanupService::class.java)
  private val lastTimeCheck = AtomicLong(TimeUtils.getTimeFast())
  private val inProgress = AtomicBoolean(false)
  private val photosPerQuery = 100

  open suspend fun tryToStartCleaningRoutine() {
    val now = TimeUtils.getTimeFast()
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
      val now = TimeUtils.getTimeFast()
      logger.debug("Start cleanDatabaseAndPhotos routine")

      val markedAsDeletedCount = photoInfoRepository.markAsDeletedPhotosUploadedEarlierThan(
        now - uploadedEarlierThanTimeDelta,
        now,
        photosPerQuery
      )

      if (markedAsDeletedCount == -1) {
        logger.debug("Could not mark photos as deleted")
        return
      }

      logger.debug("Marked as deleted $markedAsDeletedCount photos")

      photoInfoRepository.cleanDatabaseAndPhotos(
        now - deletedEarlierThanTimeDelta,
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