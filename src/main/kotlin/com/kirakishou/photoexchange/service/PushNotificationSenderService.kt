package com.kirakishou.photoexchange.service

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.entity.PhotoInfo
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
import java.time.Duration
import kotlin.coroutines.CoroutineContext


open class PushNotificationSenderService(
  private val client: WebClient,
  private val userInfoRepository: UserInfoRepository,
  private val jsonConverterService: JsonConverterService
) : CoroutineScope {
  private val logger = LoggerFactory.getLogger(PushNotificationSenderService::class.java)
  private val job = Job()
  private val mutex = Mutex()
  private val chunkSize = 4
  private val dispatcher = newFixedThreadPoolContext(chunkSize, "push-sender")
  private val maxTimeoutSeconds = 10L
  private val minExpiresInSeconds = 30L

  //these are just notifications not some important data so it's not a problem if we loose them due to a server crash
  private val requests = LinkedHashSet<String>(1024)

  private val googleCredential = GoogleCredential
    .fromStream(ClassPathResource("service-account.json").inputStream)
    .createScoped(SCOPES)

  override val coroutineContext: CoroutineContext
    get() = job + dispatcher

  private val requestActor = actor<Unit>(capacity = Channel.RENDEZVOUS, context = dispatcher) {
    consumeEach {
      startSendingPushNotifications()
    }
  }

  open fun enqueue(photoInfo: PhotoInfo) {
    launch {
      mutex.withLock { requests.add(photoInfo.userId) }
      requestActor.offer(Unit)
    }
  }

  private suspend fun startSendingPushNotifications() {
    val accessToken = getAccessToken()
    if (accessToken.isEmpty()) {
      logger.debug("Access token is empty")
      return
    }

    val url = BASE_URL + FCM_SEND_ENDPOINT
    val chunked = mutex.withLock {
      val copyOfRequests = requests.clone() as LinkedHashSet<String>
      copyOfRequests.chunked(chunkSize)
    }

    val toBeRemoveList = mutableListOf<String>()

    try {
      for (chunk in chunked) {
        //only requests that were executed successfully will be removed from the requests set
        try {
          toBeRemoveList.addAll(processChunk(chunk, url, accessToken))
        } catch (error: Throwable) {
          logger.error("Error while processing chunk of notifications", error)
        }
      }
    } finally {
      mutex.withLock { requests.removeAll(toBeRemoveList) }
    }
  }

  /**
   * Returns a list of userIds which requests were successful
   * */
  private suspend fun processChunk(chunk: List<String>, url: String, accessToken: String): List<String> {
    return chunk
      //TODO: change to getManyFirebaseTokens
      .map { userId -> userInfoRepository.getFirebaseToken(userId) to userId }
      /**
       * Filter empty tokens and NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN.
       * User may not have google play services installed, so in this case we just won't send them any push notifications
       * */
      .filter { (token, _) -> token.isNotEmpty() }
      //Send push notifications in parallel
      .map { (token, userId) -> async { sendPushNotification(url, userId, accessToken, token) } }
      //Await for their completion
      .map { result -> result.await() }
      //Filter all unsuccessful
      .filter { (result, _) -> result }
      //Return successful's userIds
      .map { (_, userId) -> userId }
  }

  private suspend fun sendPushNotification(
    url: String,
    userId: String,
    accessToken: String,
    userToken: String
  ): Pair<Boolean, String> {
    //if token is NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN then stop this request and return true immediately
    if (userToken == NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN) {
      return true to userId
    }

    val packet = Packet(Message(userToken, PhotoExchangedData()))
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
      return false to userId
    }

    if (!response.statusCode().is2xxSuccessful) {
      logger.debug("Status code is not 2xxSuccessful (${response.statusCode()})")
      return false to userId
    }

    logger.debug("PushNotification sent")
    return true to userId
  }

  private fun getAccessToken(): String {
    return try {
      //if expiresInSeconds is not null and accessToken is not null and we have 30 more seconds before token expires - return old token
      //otherwise - refresh token
      if (googleCredential.expiresInSeconds != null
        && googleCredential.accessToken != null
        && googleCredential.expiresInSeconds > minExpiresInSeconds) {
        return googleCredential.accessToken
      }

      googleCredential.refreshToken()
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
    val token: String,

    @Expose
    @SerializedName("data")
    val data: AbstractData
  )

  data class PhotoExchangedData(
    @Expose
    @SerializedName("photo_exchanged")
    val value: Boolean = true
  ) : AbstractData()

  companion object {
    private val BASE_URL = "https://fcm.googleapis.com"
    private val FCM_SEND_ENDPOINT = "/v1/projects/${ServerSettings.PROJECT_ID}/messages:send"

    private val MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private val SCOPES = listOf(MESSAGING_SCOPE)

    const val NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN = "NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN"
  }
}