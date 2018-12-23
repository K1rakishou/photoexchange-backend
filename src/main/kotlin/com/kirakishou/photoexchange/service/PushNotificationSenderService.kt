package com.kirakishou.photoexchange.service

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import core.SharedConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import kotlin.coroutines.CoroutineContext


open class PushNotificationSenderService(
  private val webClientService: WebClientService,
  private val userInfoRepository: UserInfoRepository,
  private val photoInfoRepository: PhotoInfoRepository,
  private val googleCredentialsService: GoogleCredentialsService,
  private val jsonConverterService: JsonConverterService
) : CoroutineScope {
  private val logger = LoggerFactory.getLogger(PushNotificationSenderService::class.java)
  private val job = Job()
  private val mutex = Mutex()
  private val chunkSize = 4
  private val dispatcher = newFixedThreadPoolContext(chunkSize, "push-sender")
  private val maxTimeoutSeconds = 10L

  //these are just notifications not some important data so it's not a problem if we loose them due to a server crash
  private val requests = LinkedHashSet<PhotoInfo>(1024)

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
      if (token.isNotEmpty() && token != SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN) {
        mutex.withLock { requests.add(photoInfo) }
      } else {
        logger.debug("FirebaseToken is ${SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN}, skipping it")
      }

      requestActor.offer(Unit)
    }
  }

  private suspend fun startSendingPushNotifications() {
    val requestsCopy = mutex.withLock { requests.clone() as LinkedHashSet<PhotoInfo> }

    if (requestsCopy.isEmpty()) {
      logger.debug("No requests")
      return
    }

    val accessToken = googleCredentialsService.getAccessToken()
    if (accessToken.isEmpty()) {
      logger.debug("Access token is empty")
      return
    }

    try {
      for (chunk in requestsCopy.chunked(chunkSize)) {
        //only requests that were executed successfully will be removed from the requests set
        try {
          chunk
            .map { photo -> async { processRequest(photo, accessToken) } }
            .forEach { it.await() }
        } catch (error: Throwable) {
          logger.error("Error while processing chunk of notifications", error)
        }
      }
    } finally {
      //clear requests regardless of the result
      mutex.withLock { requests.clear() }
    }
  }

  private suspend fun processRequest(myPhoto: PhotoInfo, accessToken: String) {
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

    if (firebaseToken == SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN) {
      throw RuntimeException("firebase token is ${SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN}. This should not happen!")
    }

    val data = NewReceivedPhoto(
      myPhoto.photoName,
      theirPhoto.photoName,
      theirPhoto.lon.toString(),
      theirPhoto.lat.toString(),
      theirPhoto.uploadedOn.toString()
    )

    val packet = Packet(Message(firebaseToken, data))
    val body = jsonConverterService.toJson(packet)

    //TODO: find out whether there is a way to send notifications in batches and not one at a time
    try {
      if (webClientService.sendPushNotification(accessToken, body, maxTimeoutSeconds).awaitFirst()) {
        logger.debug("PushNotification sent")
      }
    } catch (error: Throwable) {
      logger.error("Exception while executing web request", error)
    }
  }

  /** Test methods **/
  fun testGetRequests(): LinkedHashSet<PhotoInfo> {
    return requests
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

  data class NewReceivedPhoto(
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
}