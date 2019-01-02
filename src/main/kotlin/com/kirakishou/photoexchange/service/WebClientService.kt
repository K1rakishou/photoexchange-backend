package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.FileWrapper
import com.kirakishou.photoexchange.core.Photo
import com.kirakishou.photoexchange.extensions.deleteIfExists
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.io.File
import java.time.Duration

open class WebClientService(
  private val client: WebClient,
  private val diskManipulationService: DiskManipulationService
) {
  private val logger = LoggerFactory.getLogger(WebClientService::class.java)

  open suspend fun downloadLocationMap(
    photo: Photo,
    photoMapName: String,
    maxTimeoutSeconds: Long
  ): FileWrapper {
    try {
      val lon = photo.lon
      val lat = photo.lat
      val requestString = String.format(requestStringFormat, lon, lat, lon, lat)

      logger.debug("[$photoMapName], Trying to get map from google services")

      val dataBufferList = client.get()
        .uri(requestString)
        .exchange()
        .timeout(Duration.ofSeconds(maxTimeoutSeconds))
        .doOnNext { response ->
          if (!response.statusCode().is2xxSuccessful) {
            throw StaticMapDownloaderService.ResponseIsNot2xxSuccessful(
              "[$photoMapName], Response status code is not 2xxSuccessful (${response.statusCode()})"
            )
          }
        }
        .flatMapMany { response ->
          response.body(BodyExtractors.toDataBuffers())
        }
        .collectList()
        .awaitFirst()

      val outFile = File("${ServerSettings.FILE_DIR_PATH}\\$photoMapName")

      try {
        diskManipulationService.copyDataBuffersToFile(dataBufferList, outFile)
        return FileWrapper(outFile)
      } catch (error: Throwable) {
        outFile.deleteIfExists()
        throw error
      }
    } catch (error: Throwable) {
      logger.error("Error while trying to download static map", error)
      return FileWrapper()
    }
  }

  open fun sendPushNotification(
    accessToken: String,
    notificationBody: String,
    maxTimeoutSeconds: Long
  ): Mono<Boolean> {
    return client.post()
      .uri(URL)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer $accessToken")
      .body(BodyInserters.fromObject(notificationBody))
      .exchange()
      .timeout(Duration.ofSeconds(maxTimeoutSeconds))
      .map { it.statusCode() }
      .map { statusCode ->
        if (!statusCode.is2xxSuccessful) {
          logger.debug("Status code is not 2xxSuccessful ($statusCode)")
          return@map false
        }

        return@map true
      }
  }

  companion object {
    //apparently mapbox uses longitude as the first parameter and latitude as the second (as opposite to google maps)
    const val lonLatFormat = "%9.7f,%9.7f"
    val requestStringFormat = "https://api.mapbox.com/styles/v1/mapbox/streets-v10/static/" +
      "pin-l-x+050c09($lonLatFormat)/$lonLatFormat,8/600x600?access_token=${ServerSettings.MAPBOX_ACCESS_TOKEN}"

    private val BASE_URL = "https://fcm.googleapis.com"
    private val FCM_SEND_ENDPOINT = "/v1/projects/${ServerSettings.PROJECT_ID}/messages:send"
    private val URL = BASE_URL + FCM_SEND_ENDPOINT
  }
}