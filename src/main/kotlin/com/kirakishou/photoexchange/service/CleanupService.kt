package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.UploadPhotoHandler
import com.kirakishou.photoexchange.util.TimeUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

class CleanupService(
  private val photoInfoRepository: PhotoInfoRepository,
  private val diskManipulationService: DiskManipulationService
) {
  private val logger = LoggerFactory.getLogger(UploadPhotoHandler::class.java)
  private val lastTimeCheck = AtomicLong(0)
  private val photosPerQuery = 100

  suspend fun deleteOldPhotos() {
    val now = TimeUtils.getTimeFast()
    val oldTime = lastTimeCheck.get()

    if (now - lastTimeCheck.get() > ServerSettings.OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL) {
      if (lastTimeCheck.compareAndSet(oldTime, now)) {
        cleanDatabaseAndPhotos(now)
      }
    }
  }

  private suspend fun cleanDatabaseAndPhotos(now: Long) {
    if (!photoInfoRepository.markAsDeletedPhotosOlderThan(now - ServerSettings.PHOTOS_OLDER_THAN, now, photosPerQuery)) {
      logger.debug("Could not mark photos as deleted")
      return
    }

    val photosToDelete = photoInfoRepository.findOlderThan(now - ServerSettings.DELETED_PHOTOS_OLDER_THAN, photosPerQuery)
    if (photosToDelete.isEmpty()) {
      return
    }

    logger.debug("Found ${photosToDelete.size} photos to delete")

    val ids = photosToDelete.map { it.photoId }
    if (!photoInfoRepository.cleanUp(ids)) {
      return
    }

    logger.debug("Found ${ids.size} photo ids to deletePhotoWithFile")

    for (photoInfo in photosToDelete) {
      if (!photoInfoRepository.delete(photoInfo)) {
        logger.error("Could not deletePhotoWithFile photo ${photoInfo.photoName}")
        continue
      }

      diskManipulationService.deleteAllPhotoFiles(photoInfo.photoName)
    }
  }

}