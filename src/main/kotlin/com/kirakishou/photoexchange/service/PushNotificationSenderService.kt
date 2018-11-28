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
  private val logger = LoggerFactory.getLogger(StaticMapDownloaderService::class.java)
  private val job = Job()
  private val mutex = Mutex()
  private val chunkSize = 4
  private val maxTimeoutSeconds = 10L
  private val dispatcher = newFixedThreadPoolContext(chunkSize, "push-sender")

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

    for (chunk in chunked) {
      toBeRemoveList.addAll(processChunk(chunk, url, accessToken))
    }

    mutex.withLock { requests.removeAll(toBeRemoveList) }
  }

  /**
   * Return a list of userIds which requests were successful
   * */
  private suspend fun processChunk(chunk: List<String>, url: String, accessToken: String): List<String> {
    return chunk
      .map { userId -> userInfoRepository.getFirebaseToken(userId) to userId }
      //filter empty tokens
      .filter { (token, _) -> token.isEmpty() }
      //send push notifications in parallel
      .map { (token, userId) -> async { sendPushNotification(url, userId, accessToken, token) } }
      //await for their completion
      .map { result -> result.await() }
      //filter all unsuccessful
      .filter { (result, _) -> result }
      //return successful's userIds
      .map { (_, userId) -> userId }
  }

  private suspend fun sendPushNotification(
    url: String,
    userId: String,
    accessToken: String,
    userToken: String
  ): Pair<Boolean, String> {
    val packet = Packet(Message(userToken, TestData("test1", "test2")))
    val body = jsonConverterService.toJson(packet)

    //TODO: find out whether there is a way to send notifications in batches not by one at a time
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

    return true to userId
  }

  private fun getAccessToken(): String {
    return try {
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

  data class TestData(
    @Expose
    @SerializedName("string1")
    val string1: String,

    @Expose
    @SerializedName("string2")
    val string2: String
  ) : AbstractData()

  companion object {
    private val BASE_URL = "https://fcm.googleapis.com"
    private val FCM_SEND_ENDPOINT = "/v1/projects/${ServerSettings.PROJECT_ID}/messages:send"

    private val MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private val SCOPES = listOf(MESSAGING_SCOPE)
  }
}