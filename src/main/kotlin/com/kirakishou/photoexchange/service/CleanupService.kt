package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.util.TimeUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

class CleanupService(
  private val photoInfoRepository: PhotoInfoRepository,
  private val diskManipulationService: DiskManipulationService
) {
  private val logger = LoggerFactory.getLogger(CleanupService::class.java)
  private val lastTimeCheck = AtomicLong(TimeUtils.getTimeFast())
  private val photosPerQuery = 100

  suspend fun tryToStartCleaningRoutine(forced: Boolean) {
    val now = TimeUtils.getTimeFast()
    val oldTime = lastTimeCheck.get()

    val timeHasCome = ((now - lastTimeCheck.get() > ServerSettings.OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL) &&
      (lastTimeCheck.compareAndSet(oldTime, now)))

    if (forced || timeHasCome) {
      logger.debug("Start cleanDatabaseAndPhotos routine")

      try {
        cleanDatabaseAndPhotos(now)
      } catch (error: Throwable) {
        logger.error("Error while cleaning up", error)
      } finally {
        logger.debug("End cleanDatabaseAndPhotos routine")
      }
    }
  }

  //TODO:
  //delete photos by pairs not by one! I.e. photo and the other one it's been exchanged with
  //otherwise inconsistency may occur!
  private suspend fun cleanDatabaseAndPhotos(now: Long) {
    if (!photoInfoRepository.markAsDeletedPhotosOlderThan(now - ServerSettings.PHOTOS_OLDER_THAN, now, photosPerQuery)) {
      logger.debug("Could not mark photos as deleted")
      return
    }

    val photosToDelete = photoInfoRepository.findOlderThan(now - ServerSettings.DELETED_PHOTOS_OLDER_THAN, photosPerQuery)
    if (photosToDelete.isEmpty()) {
      logger.debug("No photos to delete")
      return
    }

    logger.debug("Found ${photosToDelete.size} photos to delete")

    for (photoInfo in photosToDelete) {
      logger.debug("Deleting ${photoInfo.photoName}")

      if (!photoInfoRepository.delete(photoInfo)) {
        logger.error("Could not deletePhotoWithFile photo ${photoInfo.photoName}")
        continue
      }

      diskManipulationService.deleteAllPhotoFiles(photoInfo.photoName)
    }
  }

}