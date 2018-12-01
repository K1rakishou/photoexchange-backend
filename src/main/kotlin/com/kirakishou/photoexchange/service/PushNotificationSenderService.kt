package com.kirakishou.photoexchange.service

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.lang.RuntimeException
import java.time.Duration
import kotlin.coroutines.CoroutineContext


open class PushNotificationSenderService(
  private val client: WebClient,
  private val userInfoRepository: UserInfoRepository,
  private val photoInfoRepository: PhotoInfoRepository,
  private val jsonConverterService: JsonConverterService
) : CoroutineScope {
  private val logger = LoggerFactory.getLogger(PushNotificationSenderService::class.java)
  private val job = Job()
  private val mutex = Mutex()
  private val chunkSize = 4
  private val dispatcher = newFixedThreadPoolContext(chunkSize, "push-sender")
  private val maxTimeoutSeconds = 10L
  private val minTimeUntilRefresh = 30L

  //these are just notifications not some important data so it's not a problem if we loose them due to a server crash
  private val requests = LinkedHashSet<PhotoInfo>(1024)

  private val googleCredential = GoogleCredential
    //your service-account.json should be in the resources directory (https://cloud.google.com/iam/docs/creating-managing-service-account-keys)
    .fromStream(ClassPathResource("service-account.json").inputStream)
    .createScoped(SCOPES)

  override val coroutineContext: CoroutineContext
    get() = job + dispatcher

  init {
    if (!ClassPathResource("service-account.json").exists()) {
      throw RuntimeException("\"service-account.json\" should exist in the resources directory")
    }
  }

  private val requestActor = actor<Unit>(capacity = Channel.RENDEZVOUS, context = dispatcher) {
    consumeEach {
      startSendingPushNotifications()
    }
  }

  open fun enqueue(photoInfo: PhotoInfo) {
    launch {
      val token = userInfoRepository.getFirebaseToken(photoInfo.userId)
      if (token.isNotEmpty() && token != NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN) {
        mutex.withLock { requests.add(photoInfo) }
      }

      requestActor.offer(Unit)
    }
  }

  private suspend fun startSendingPushNotifications() {
    if (requests.isEmpty()) {
      logger.debug("No requests")
      return
    }

    val accessToken = getAccessToken()
    if (accessToken.isEmpty()) {
      logger.debug("Access token is empty")
      return
    }

    val url = BASE_URL + FCM_SEND_ENDPOINT
    val requestsCopy = mutex.withLock { requests.clone() as LinkedHashSet<PhotoInfo> }

    try {
      for (chunk in requestsCopy.chunked(chunkSize)) {
        //only requests that were executed successfully will be removed from the requests set
        try {
          chunk
            .map { photo -> async { processRequest(photo, url, accessToken) } }
            .forEach { it.await() }
        } catch (error: Throwable) {
          logger.error("Error while processing chunk of notifications", error)
        }
      }
    } finally {
      //clear request regardless of the result
      mutex.withLock { requests.clear() }
    }
  }

  private suspend fun processRequest(myPhoto: PhotoInfo, url: String, accessToken: String) {
    val theirPhoto = photoInfoRepository.findOneById(myPhoto.exchangedPhotoId)
    if (theirPhoto.isEmpty()) {
      logger.debug("No photo with id ${myPhoto.exchangedPhotoId}, photoName = ${myPhoto.photoName}")
      return
    }

    val firebaseToken = userInfoRepository.getFirebaseToken(myPhoto.userId)
    if (firebaseToken.isEmpty()) {
      logger.debug("Firebase token is empty")
      return
    }

    if (firebaseToken == NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN) {
      throw RuntimeException("firebase token is $NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN. This should not happen here!!!")
    }

    val packet = PhotoExchangedData(
      theirPhoto.photoName,
      myPhoto.photoName,
      theirPhoto.lon.toString(),
      theirPhoto.lat.toString(),
      theirPhoto.uploadedOn.toString()
    )

    sendPushNotification(url, accessToken, firebaseToken, packet)
  }

  private suspend fun sendPushNotification(
    url: String,
    accessToken: String,
    firebaseToken: String,
    data: PhotoExchangedData
  ) {
    val packet = Packet(Message(firebaseToken, data))
    val body = jsonConverterService.toJson(packet)

    //TODO: find out whether there is a way to send notifications in batches and not one at a time
    val response = try {
      client.post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .header("Authorization", "Bearer $accessToken")
        .body(BodyInserters.fromObject(body))
        .exchange()
        .timeout(Duration.ofSeconds(maxTimeoutSeconds))
        .awaitFirst()
    } catch (error: Throwable) {
      logger.error("Exception while executing web request", error)
      return
    }

    if (!response.statusCode().is2xxSuccessful) {
      logger.debug("Status code is not 2xxSuccessful (${response.statusCode()})")
      return
    }

    logger.debug("PushNotification sent")
    return
  }

  private fun getAccessToken(): String {
    return try {
      //if expiresInSeconds is not null and accessToken is not null and we have 30 more seconds before token expires - return old token
      //otherwise - refresh token
      if (googleCredential.expiresInSeconds != null
        && googleCredential.accessToken != null
        && googleCredential.expiresInSeconds > minTimeUntilRefresh) {
        return googleCredential.accessToken
      }

      logger.debug("Refreshing token...")
      googleCredential.refreshToken()
      logger.debug("Done refreshing token...")

      googleCredential.accessToken
    } catch (error: Throwable) {
      logger.error("Could not acquire access token", error)
      ""
    }
  }

  abstract class AbstractData

  data class Packet(
    @Expose
    @SerializedName("message")
    val message: Message
  )

  data class Message(
    @Expose
    @SerializedName("token")
    val firebaseToken: String,

    @Expose
    @SerializedName("data")
    val data: AbstractData
  )

  data class PhotoExchangedData(
    @Expose
    @SerializedName("uploaded_photo_name")
    val uploadedPhotoName: String,

    @Expose
    @SerializedName("received_photo_name")
    val receivedPhotoName: String,

    @Expose
    @SerializedName("lon")
    val lon: String,

    @Expose
    @SerializedName("lat")
    val lat: String,

    @Expose
    @SerializedName("uploaded_on")
    val uploadedOn: String
  ) : AbstractData()

  companion object {
    private val BASE_URL = "https://fcm.googleapis.com"
    private val FCM_SEND_ENDPOINT = "/v1/projects/${ServerSettings.PROJECT_ID}/messages:send"

    private val MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private val SCOPES = listOf(MESSAGING_SCOPE)

    const val NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN = "NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN"
  }
}