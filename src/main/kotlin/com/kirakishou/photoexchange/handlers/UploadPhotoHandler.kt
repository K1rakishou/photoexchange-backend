package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.repository.BanListRepository
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.database.repository.UsersRepository
import com.kirakishou.photoexchange.extensions.containsAllParts
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.*
import com.kirakishou.photoexchange.util.SecurityUtils
import com.kirakishou.photoexchange.util.TimeUtils
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.request.UploadPhotoPacket
import net.response.UploadPhotoResponse
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.io.File
import java.io.IOException

class UploadPhotoHandler(
  private val photosRepository: PhotosRepository,
  private val usersRepository: UsersRepository,
  private val banListRepository: BanListRepository,
  private val staticMapDownloaderService: StaticMapDownloaderService,
  private val pushNotificationSenderService: PushNotificationSenderService,
  private val remoteAddressExtractorService: RemoteAddressExtractorService,
  private val diskManipulationService: DiskManipulationService,
  private val cleanupService: CleanupService,
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(UploadPhotoHandler::class.java)
  private val mutex = Mutex()
  private val PACKET_PART_KEY = "packet"
  private val PHOTO_PART_KEY = "photo"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono { handleInternal(request) }
      .flatMap { it }
  }

  //test coverage doesn't work with lambdas
  private suspend fun handleInternal(request: ServerRequest): Mono<ServerResponse> {
    logger.debug("New UploadPhoto request")

    try {
      val address = remoteAddressExtractorService.extractRemoteAddress(request)
      val ipHash = getIpAddressHash(address)

      if (banListRepository.isBanned(IpHash(ipHash))) {
        logger.error("User is banned. ipHash = $ipHash")
        return formatResponse(HttpStatus.FORBIDDEN, UploadPhotoResponse.fail(ErrorCode.YouAreBanned))
      }

      val multiValueMap = request.body(BodyExtractors.toMultipartData()).awaitSingle()
      if (!multiValueMap.containsAllParts(PACKET_PART_KEY, PHOTO_PART_KEY)) {
        logger.error("Request does not contain one of the required path variables")
        return formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.BadRequest))
      }

      val packetParts = try {
        collectPart(multiValueMap, PACKET_PART_KEY, SharedConstants.MAX_PACKET_SIZE).awaitSingle()
      } catch (error: EmptyPacket) {
        logger.error("Packet part is empty")
        return formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.RequestPartIsEmpty))
      } catch (error: RequestSizeExceeded) {
        logger.error("Packet part's size exceeds maxSize, requestSize = ${error.requestSize}")
        return formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.ExceededMaxPacketSize))
      }

      val packet = jsonConverter.fromJson<UploadPhotoPacket>(packetParts)
      if (!isPacketOk(packet)) {
        logger.error("One or more of the packet's fields are incorrect")
        return formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.BadRequest))
      }

      val userId = usersRepository.getUserIdByUserUuid(UserUuid(packet.userUuid))
      if (userId.isEmpty()) {
        logger.error("Account with userUuid ${packet.userUuid} does not exist!")
        return formatResponse(HttpStatus.FORBIDDEN, UploadPhotoResponse.fail(ErrorCode.AccountNotFound))
      }

      //user should not be able to send photo without creating default account with firebase token first
      val token = usersRepository.getFirebaseToken(UserUuid(packet.userUuid))
      if (token.isEmpty()) {
        logger.error("User does not have firebase token yet!")
        return formatResponse(HttpStatus.FORBIDDEN, UploadPhotoResponse.fail(ErrorCode.UserDoesNotHaveFirebaseToken))
      }

      val photoParts = try {
        collectPart(multiValueMap, PHOTO_PART_KEY, SharedConstants.MAX_PHOTO_SIZE).awaitSingle()
      } catch (error: EmptyPacket) {
        logger.error("Photo part is empty")
        return formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.RequestPartIsEmpty))
      } catch (error: RequestSizeExceeded) {
        logger.error("Photo part's size exceeds maxSize, requestSize = ${error.requestSize}")
        return formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.ExceededMaxPhotoSize))
      }

      val newUploadingPhoto = photosRepository.save(
        userId,
        packet.lon,
        packet.lat,
        packet.isPublic,
        TimeUtils.getTimeFast(),
        IpHash(ipHash)
      )

      if (newUploadingPhoto.isEmpty()) {
        logger.error("Could not save a photoInfo")
        return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.DatabaseError))
      }

      try {
        if (!staticMapDownloaderService.enqueue(newUploadingPhoto.photoId)) {
          logger.error("Could not enqueue photo in locationMapReceiverService")

          deletePhotoWithFile(newUploadingPhoto)
          return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.DatabaseError))
        }

        val tempFile = saveTempFile(photoParts, newUploadingPhoto)
        if (tempFile.isEmpty()) {
          logger.error("Could not save file to disk")

          deletePhotoWithFile(newUploadingPhoto)
          return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.ServerDiskError))
        }

        try {
          diskManipulationService.resizeAndSavePhotos(tempFile, newUploadingPhoto)
        } catch (error: Throwable) {
          logger.error("Unknown error", error)
          deletePhotoWithFile(newUploadingPhoto)

          return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.ServerResizeError))
        } finally {
          tempFile.deleteIfExists()
        }

        try {
          cleanupService.tryToStartCleaningRoutine()
        } catch (error: Throwable) {
          logger.error("Error while cleaning up (cleanDatabaseAndPhotos)", error)
        }

        val exchangedPhoto = try {
          //FIXME: this works, but it's slow. Gotta figure out how to exchange photos without locks
          //Maybe should make another service to do exchanging?
          //Every time a new photos is uploaded - trigger the service. The service than check whether there is an old not exchanged photo
          //If there is - do the exchange, otherwise wait for another photo
          mutex.withLock { photosRepository.tryDoExchange(UserUuid(packet.userUuid), newUploadingPhoto) }
        } catch (error: Throwable) {
          logger.error("Unknown error while trying to do photo exchange", error)

          deletePhotoWithFile(newUploadingPhoto)
          return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.DatabaseError))
        }

        logger.debug("Photo has been successfully uploaded")

        if (!exchangedPhoto.isEmpty()) {
          pushNotificationSenderService.enqueue(exchangedPhoto)
        }

        val response = UploadPhotoResponse.success(
          newUploadingPhoto.photoId.id,
          newUploadingPhoto.photoName.name,
          newUploadingPhoto.uploadedOn
        )

        return formatResponse(HttpStatus.OK, response)
      } catch (error: Throwable) {
        deletePhotoWithFile(newUploadingPhoto)

        throw error
      }
    } catch (error: Throwable) {
      logger.error("Unknown error", error)
      return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.UnknownError))
    }
  }

  private fun isPacketOk(packet: UploadPhotoPacket): Boolean {
    if (packet.lon == null || packet.lon < -180.0 || packet.lon > 180.0) {
      logger.debug("Bad param lon (${packet.lon})")
      return false
    }

    if (packet.lat == null || packet.lat < -90.0 || packet.lat > 90.0) {
      logger.debug("Bad param lat (${packet.lat})")
      return false
    }

    if (packet.userUuid == null || packet.userUuid.isEmpty() || packet.userUuid.length > SharedConstants.MAX_USER_UUID_LEN) {
      logger.debug("Bad param userUuid (${packet.userUuid})")
      return false
    }

    if (packet.isPublic == null) {
      logger.debug("Bad param isPublic (${packet.isPublic})")
      return false
    }

    return true
  }

  private fun getIpAddressHash(ipAddress: String): String {
    val hash = SecurityUtils.Hashing.sha3(ipAddress)

    logger.debug("ip = $ipAddress, hash = $hash")
    return hash
  }

  private fun collectPart(
    map: MultiValueMap<String, Part>,
    partName: String,
    maxSize: Long
  ): Mono<MutableList<DataBuffer>> {
    val contentFlux = map.getFirst(partName)!!
      .content()
      .publish()
      .autoConnect(2)

    val totalSizeFlux = contentFlux
      .map { it.readableByteCount().toLong() }
      .doOnNext { count ->
        if (count == 0L) {
          throw EmptyPacket()
        }
      }
      .scan { acc, count -> acc + count }

    return contentFlux.zipWith(totalSizeFlux)
      .doOnNext {
        if (it.t2 > maxSize) {
          throw RequestSizeExceeded(it.t2)
        }
      }
      .map { it.t1 }
      .buffer()
      .single()
  }

  private fun saveTempFile(photoChunks: MutableList<DataBuffer>, photo: Photo): FileWrapper {
    val filePath = "${ServerSettings.FILE_DIR_PATH}\\${photo.photoName}"
    val outFile = File(filePath)

    try {
      diskManipulationService.copyDataBuffersToFile(photoChunks, outFile)
    } catch (e: IOException) {
      logger.error("Error while trying to save file to the disk", e)

      return FileWrapper()
    }

    return FileWrapper(outFile)
  }

  private suspend fun deletePhotoWithFile(photo: Photo) {
    try {
      photosRepository.delete(photo.photoId)
    } catch (error: Throwable) {
      logger.debug("Could not delete photo with id (${photo.photoId.id})")
      return
    }

    diskManipulationService.deleteAllPhotoFiles(photo.photoName)
  }
}

















