package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.DatabaseTransactionException
import com.kirakishou.photoexchange.core.LocationMap
import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.database.repository.LocationMapRepository
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

open class StaticMapDownloaderService(
  private val webClientService: WebClientService,
  private val photosRepository: PhotosRepository,
  private val locationMapRepository: LocationMapRepository,
  private val diskManipulationService: DiskManipulationService,
  private val dispatcher: CoroutineDispatcher
) : CoroutineScope {
  private val logger = LoggerFactory.getLogger(StaticMapDownloaderService::class.java)
  private val job = Job()
  private val REQUESTS_PER_BATCH = 100
  private val MAX_TIMEOUT_SECONDS = 15L
  private val CHUNCKS_COUNT = 4

  private val repeatTimesList = listOf(
    0L,
    TimeUnit.SECONDS.toMillis(20),    //20 seconds
    TimeUnit.SECONDS.toMillis(60),    //60 seconds
    TimeUnit.SECONDS.toMillis(300),   //300 seconds (5 minutes)
    TimeUnit.HOURS.toMillis(1),       //one hour
    TimeUnit.HOURS.toMillis(4),       //4 hours
    TimeUnit.DAYS.toMillis(1)         //1 day
  )

  override val coroutineContext: CoroutineContext
    get() = job + dispatcher

  private val requestActor = actor<Unit>(capacity = Channel.RENDEZVOUS, context = dispatcher) {
    consumeEach {
      startDownloadingMapFiles()
    }
  }

  fun init() {
    //check if there are not yet downloaded map files on every server start
    requestActor.offer(Unit)
  }

  open suspend fun enqueue(photoId: PhotoId): Boolean {
    val saveResult = locationMapRepository.save(photoId)

    launch(dispatcher) {
      logger.debug("Notifying the actor to process the next batch of maps")

      //try to start the downloading process regardless of the save result because there might be old requests in the queue
      //and we want to process them
      requestActor.offer(Unit)
    }

    logger.debug("Notifying done!")
    return saveResult
  }

  //this method is for tests only
  open suspend fun testEnqueue(photoId: PhotoId?): Boolean {
    if (photoId != null) {
      val saveResult = locationMapRepository.save(photoId)
      if (!saveResult) {
        return false
      }
    }

    startDownloadingMapFiles()
    return true
  }

  private suspend fun startDownloadingMapFiles() {
    val requestsBatch = locationMapRepository.getOldest(
      REQUESTS_PER_BATCH,
      TimeUtils.getTimeFast()
    )

    if (requestsBatch.isEmpty()) {
      logger.debug("No requests for map downloading")
      return
    }

    logger.debug("Found ${requestsBatch.size} requests")
    processBatch(requestsBatch.chunked(CHUNCKS_COUNT))
    logger.debug("Chunk processed, proceeding to the next one")

    //check again if there any new maps to be downloaded
    startDownloadingMapFiles()
  }

  private suspend fun processBatch(requestsBatchChunked: List<List<LocationMap>>) {
    for (chunk in requestsBatchChunked) {
      chunk
        //send concurrent requests (CHUNCKS_COUNT max) to the google servers
        .map { locationMap -> async { getLocationMap(locationMap) } }
        //all errors are being handled inside of the coroutine so we shouldn't get any exceptions here
        .forEach { result -> result.await() }
    }
  }

  /**
   * If some kind of error has happened in this function we should not do anything with queued up request,
   * just retry it some time later
   * */
  private suspend fun getLocationMap(locationMap: LocationMap): Boolean {
    try {
      val photo = photosRepository.findOneById(locationMap.photoId)
      if (photo.isEmpty()) {
        throw CouldNotFindPhotoInfo("Could not find photoInfo with photoId = ${locationMap.photoId.id}")
      }

      if (photo.isAnonymous()) {
        //photo does not have location attached to it, so just update it's state as Anonymous
        locationMapRepository.setMapAnonymous(locationMap.photoId, locationMap.id)
        return true
      }

      val photoMapName = "${photo.photoName}${ServerSettings.PHOTO_MAP_SUFFIX}"

      val outFile = webClientService.downloadLocationMap(
        photo,
        photoMapName,
        MAX_TIMEOUT_SECONDS
      )

      if (outFile.isEmpty()) {
        throw CouldNotDownloadMap("downloadLocationMap returned empty file")
      }

      try {
        locationMapRepository.setMapReady(locationMap.photoId, locationMap.id)

        logger.debug("[$photoMapName], Map has been successfully downloaded")
      } catch (error: Throwable) {
        if (!outFile.deleteIfExists()) {
          logger.warn("Could not delete file ${outFile.getFile()!!.absolutePath}")
        }

        throw error
      }
    } catch (error: Throwable) {
      logger.error("Unknown error", error)

      increaseAttemptsCountOrSetStatusFailed(locationMap)
      return false
    }

    return true
  }

  private suspend fun increaseAttemptsCountOrSetStatusFailed(locationMap: LocationMap) {
    val repeatTimeDelta = repeatTimesList.getOrElse(locationMap.attemptsCount + 1, { -1L })
    if (repeatTimeDelta == -1L) {
      //no more attempts left
      logger.debug("No more attempts left, updating location map state as failed")

      updateMapAsFailed(locationMap)
    } else {
      logger.debug("There are attempts left, increasing attempts count")

      try {
        locationMapRepository.increaseAttemptsCountAndNextAttemptTime(locationMap.photoId, repeatTimeDelta)
      } catch (error: DatabaseTransactionException) {
        logger.error("Error", error)
      }
    }
  }

  private suspend fun updateMapAsFailed(locationMap: LocationMap) {
    try {
      locationMapRepository.setMapFailed(locationMap.photoId, locationMap.id)
    } catch (error: DatabaseTransactionException) {
      logger.error("Error", error)
      return
    }

    val photoInfo = photosRepository.findOneById(locationMap.photoId)
    if (photoInfo.isEmpty()) {
      logger.error("Could not find photo by photoId (${locationMap.photoId})")
      return
    }

    try {
      diskManipulationService.replaceMapOnDiskWithNoMapAvailablePlaceholder(photoInfo.photoName)
      logger.debug("Successfully updated location map as failed for photo (${photoInfo.photoName.name})")
    } catch (error: Throwable) {
      logger.error("Could not replace map with placeholder for photo with name (${photoInfo.photoName.name})", error)
    }
  }

  class CouldNotFindPhotoInfo(message: String) : Exception(message)
  class CouldNotDownloadMap(message: String) : Exception(message)
  class ResponseIsNot2xxSuccessful(message: String) : Exception(message)
}