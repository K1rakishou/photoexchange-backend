package com.kirakishou.photoexchange.service

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.core.Photo
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.database.repository.UsersRepository
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
  private val usersRepository: UsersRepository,
  private val photosRepository: PhotosRepository,
  private val googleCredentialsService: GoogleCredentialsService,
  private val jsonConverterService: JsonConverterService,
  private val dispatcher: CoroutineDispatcher
) : CoroutineScope {
  private val logger = LoggerFactory.getLogger(PushNotificationSenderService::class.java)
  private val job = Job()
  private val mutex = Mutex()
  private val chunkSize = 4
  private val maxTimeoutSeconds = 10L

  //these are just notifications not some important data so it's not a problem if we loose them due to a server crash
  private val requests = LinkedHashSet<Photo>(1024)

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

  open fun enqueue(photo: Photo) {
    launch {
      val userUuid = usersRepository.getUserUuidByUserId(photo.userId)
      if (userUuid.isEmpty()) {
        logger.error("Could not find user with userId (${photo.userId.id})")
        return@launch
      }

      val token = usersRepository.getFirebaseToken(userUuid)
      if (token.isNotEmpty() && !token.isDefault()) {
        mutex.withLock { requests.add(photo) }
      } else {
        logger.debug("FirebaseToken is ${SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN}, skipping it")
      }

      requestActor.offer(Unit)
    }
  }

  private suspend fun startSendingPushNotifications() {
    val requestsCopy = mutex.withLock { requests.clone() as LinkedHashSet<Photo> }

    if (requestsCopy.isEmpty()) {
      logger.debug("No requests")
      return
    }

    val accessToken = googleCredentialsService.getAccessToken()
    if (accessToken.isEmpty()) {
      logger.error("Access token is empty")
      return
    }

    for (chunk in requestsCopy.chunked(chunkSize)) {
      try {
        chunk
          .map { photo -> async { processRequest(photo, accessToken) } }
          .forEach { it.await() }
      } catch (error: Throwable) {
        logger.error("Error while processing chunk of notifications", error)
      } finally {
        mutex.withLock {
          logger.debug("old requests = ${requests.map { it.photoName }}, chunk = ${chunk.map { it.photoName }}")
          requests.removeAll(chunk)
          logger.debug("after removing requests = ${requests.map { it.photoName }}")
        }
      }
    }
  }

  private suspend fun processRequest(myPhoto: Photo, accessToken: String) {
    logger.debug("Processing request for photo ${myPhoto.photoName}")

    val theirPhoto = photosRepository.findOneById(myPhoto.exchangedPhotoId.toPhotoId())
    if (theirPhoto.isEmpty()) {
      logger.error("No photo with id ${myPhoto.exchangedPhotoId}, photoName = ${myPhoto.photoName}")
      return
    }

    val userUuid = usersRepository.getUserUuidByUserId(myPhoto.userId)
    if (userUuid.isEmpty()) {
      logger.error("Could not find user with userId (${myPhoto.userId.id})")
      return
    }

    val firebaseToken = usersRepository.getFirebaseToken(userUuid)
    if (firebaseToken.isEmpty()) {
      logger.error("Firebase token is empty")
      return
    }

    if (firebaseToken.isDefault()) {
      throw RuntimeException("firebase token is ${SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN}. This should not happen!")
    }

    val data = NewReceivedPhoto(
      myPhoto.photoName.name,
      theirPhoto.photoName.name,
      theirPhoto.lon.toString(),
      theirPhoto.lat.toString(),
      theirPhoto.uploadedOn.millis.toString()
    )

    val packet = Packet(Message(firebaseToken.token, data))
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
  fun testGetRequests(): LinkedHashSet<Photo> {
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