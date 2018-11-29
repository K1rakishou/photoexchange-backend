package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.config.ServerSettings.PHOTO_MAP_SUFFIX
import com.kirakishou.photoexchange.database.repository.LocationMapRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.deleteIfExists
import com.kirakishou.photoexchange.database.entity.LocationMap
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitLast
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

open class StaticMapDownloaderService(
  private val client: WebClient,
  private val photoInfoRepository: PhotoInfoRepository,
  private val locationMapRepository: LocationMapRepository
) : CoroutineScope {
  private val logger = LoggerFactory.getLogger(StaticMapDownloaderService::class.java)
  private val job = Job()
  private val REQUESTS_PER_BATCH = 100
  private val MAX_TIMEOUT_SECONDS = 15L
  private val CHUNCKS_COUNT = 4
  private val dispatcher = newFixedThreadPoolContext(CHUNCKS_COUNT, "map-downloader")

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

  open suspend fun enqueue(photoId: Long): Boolean {
    val saveResult = locationMapRepository.save(LocationMap.create(photoId))

    launch(dispatcher) {
      logger.debug("Notifying the actor to process the next batch of maps")

      //try to start the downloading process regardless of the save result because there might be old requests in the queue
      //and we want to process them
      requestActor.offer(Unit)
    }

    logger.debug("Notifying done!")
    return !saveResult.isEmpty()
  }

  private suspend fun startDownloadingMapFiles() {
    val requestsBatch = locationMapRepository.getOldest(REQUESTS_PER_BATCH, TimeUtils.getTimeFast())
    if (requestsBatch.isEmpty()) {
      logger.debug("No requests for map downloading")
      return
    }

    logger.debug("Found ${requestsBatch.size} requests")
    processBatch(requestsBatch.chunked(CHUNCKS_COUNT))

    //check again if there any new maps to be downloaded
    logger.debug("Chunk processed, proceeding to the next one")
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
      val photoInfo = photoInfoRepository.findOneById(locationMap.photoId)
      if (photoInfo.isEmpty()) {
        throw CouldNotFindPhotoInfo("Could not find photoInfo with photoId = ${locationMap.photoId}")
      }

      val lon = photoInfo.lon
      val lat = photoInfo.lat
      val photoMapName = "${photoInfo.photoName}$PHOTO_MAP_SUFFIX"
      val requestString = String.format(requestStringFormat, lon, lat, lon, lat)

      logger.debug("[$photoMapName], Trying to get map from google services")
      val response = client.get()
        .uri(requestString)
        .exchange()
        .timeout(Duration.ofSeconds(MAX_TIMEOUT_SECONDS))
        .awaitFirst()

      //TODO: handle TOO_MANY_REQUESTS status
      if (!response.statusCode().is2xxSuccessful) {
        if (response.statusCode() == HttpStatus.FORBIDDEN) {
          logger.debug("StatusCode is FORBIDDEN. Probably should check the developer account")
        }

        if (response.statusCode() == HttpStatus.TOO_MANY_REQUESTS) {
          logger.debug("StatusCode is TOO_MANY_REQUESTS. Probably exceeded request quota")
        }

        throw ResponseIsNot2xxSuccessful("[$photoMapName], Response status code is not 2xxSuccessful (${response.statusCode()})")
      }

      val filePath = "${ServerSettings.FILE_DIR_PATH}\\$photoMapName"
      val outFile = File(filePath)

      try {
        if (!saveFileToDisk(outFile, response)) {
          throw CouldNotSaveToDiskException("[$photoMapName], Could not save file to disk")
        }

        if (!locationMapRepository.setMapReady(locationMap.photoId)) {
          throw CouldNotUpdateMapReadyFlag("[$photoMapName], Could not set mapReady flag in the DB")
        }

        if (!photoInfoRepository.updateSetLocationMapId(locationMap.photoId, locationMap.id)) {
          throw CouldNotUpdateLocationId("[$photoMapName], Could not set locationMapId flag in the DB")
        }

        logger.debug("[$photoMapName], Map has been successfully downloaded")
      } catch (error: Throwable) {
        cleanup(outFile)
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
      //TODO
      // probably should set here some kind of default image for a case when we could not
      // download a map from the google servers and wasted all of the attempts

      if (!locationMapRepository.setMapFailed(locationMap.photoId)) {
        logger.error("Could not set map status as Failed")
      }
    } else {
      val nextAttemptTime = TimeUtils.getTimeFast() + repeatTimeDelta
      if (!locationMapRepository.increaseAttemptsCountAndNextAttemptTime(locationMap.photoId, nextAttemptTime)) {
        logger.error("Could not increase attempts count")
      }
    }
  }

  private fun cleanup(outFile: File) {
    outFile.deleteIfExists()
  }

  private suspend fun saveFileToDisk(outFile: File, response: ClientResponse): Boolean {
    try {
      outFile.outputStream().use { outputStream ->
        response.body(BodyExtractors.toDataBuffers())
          .doOnNext { chunk ->
            chunk.asInputStream().use { inputStream ->
              val chunkSize = inputStream.available()
              val buffer = ByteArray(chunkSize)

              //copy chunks from one stream to another
              inputStream.read(buffer, 0, chunkSize)
              outputStream.write(buffer, 0, chunkSize)
            }
          }.awaitLast()
      }

    } catch (error: Throwable) {
      logger.error("Error while trying to store static map image onto the disk", error)
      return false
    }

    return true
  }

  class CouldNotFindPhotoInfo(message: String) : Exception(message)
  class ResponseIsNot2xxSuccessful(message: String) : Exception(message)
  class CouldNotSaveToDiskException(message: String) : Exception(message)
  class CouldNotUpdateMapReadyFlag(message: String) : Exception(message)
  class CouldNotUpdateLocationId(message: String) : Exception(message)

  companion object {
    //apparently mapbox uses longitude as the first parameter and latitude as the second (as opposite to google maps)
    const val lonLatFormat = "%9.7f,%9.7f"
    val requestStringFormat ="https://api.mapbox.com/styles/v1/mapbox/streets-v10/static/pin-l-x+050c09($lonLatFormat)/$lonLatFormat,8/600x600?access_token=${ServerSettings.MAPBOX_ACCESS_TOKEN}"
  }
}