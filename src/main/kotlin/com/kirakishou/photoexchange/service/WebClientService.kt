package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

open class WebClientService(
  private val client: WebClient
) {
  private val logger = LoggerFactory.getLogger(WebClientService::class.java)

  open fun downloadLocationMap(
    photoInfo: PhotoInfo,
    photoMapName: String,
    maxTimeoutSeconds: Long
  ): Flux<DataBuffer> {
    val lon = photoInfo.lon
    val lat = photoInfo.lat
    val requestString = String.format(requestStringFormat, lon, lat, lon, lat)

    logger.debug("[$photoMapName], Trying to get map from google services")

    return client.get()
      .uri(requestString)
      .exchange()
      .timeout(Duration.ofSeconds(maxTimeoutSeconds))
      .doOnNext { response ->
        if (!response.statusCode().is2xxSuccessful) {
          if (response.statusCode() == HttpStatus.FORBIDDEN) {
            logger.debug("StatusCode is FORBIDDEN. Probably should check the developer account")
          }

          if (response.statusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            logger.debug("StatusCode is TOO_MANY_REQUESTS. Probably exceeded request quota")
          }

          throw StaticMapDownloaderService.ResponseIsNot2xxSuccessful(
            "[$photoMapName], Response status code is not 2xxSuccessful (${response.statusCode()})"
          )
        }
      }
      .flatMapMany { response ->
        response.body(BodyExtractors.toDataBuffers())
      }
  }

  open fun sendPushNotification(
    accessToken: String,
    notificationBody: String,
    maxTimeoutSeconds: Long
  ): Mono<Unit> {
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
        } else {
          logger.debug("PushNotification sent")
        }
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