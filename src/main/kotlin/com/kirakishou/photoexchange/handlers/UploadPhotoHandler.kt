package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.FileWrapper
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.database.repository.BanListRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.exception.EmptyPacket
import com.kirakishou.photoexchange.exception.RequestSizeExceeded
import com.kirakishou.photoexchange.extensions.containsAllParts
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.*
import com.kirakishou.photoexchange.util.SecurityUtils
import com.kirakishou.photoexchange.util.TimeUtils
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import net.request.SendPhotoPacket
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
import java.lang.RuntimeException

class UploadPhotoHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository,
	private val userInfoRepository: UserInfoRepository,
	private val banListRepository: BanListRepository,
	private val staticMapDownloaderService: StaticMapDownloaderService,
	private val pushNotificationSenderService: PushNotificationSenderService,
	private val remoteAddressExtractorService: RemoteAddressExtractorService,
	private val diskManipulationService: DiskManipulationService,
	private val cleanupService: CleanupService
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(UploadPhotoHandler::class.java)
	private val PACKET_PART_KEY = "packet"
	private val PHOTO_PART_KEY = "photo"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono {
			logger.debug("New UploadPhoto request")

			try {
        val ipHash = getIpAddressHash(remoteAddressExtractorService.extractRemoteAddress(request))
        if (banListRepository.isBanned(ipHash)) {
          logger.error("User is banned. ipHash = $ipHash")
          return@mono formatResponse(HttpStatus.FORBIDDEN, UploadPhotoResponse.fail(ErrorCode.YouAreBanned))
        }

				val multiValueMap = request.body(BodyExtractors.toMultipartData()).awaitSingle()
				if (!multiValueMap.containsAllParts(PACKET_PART_KEY, PHOTO_PART_KEY)) {
					logger.error("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.BadRequest))
				}

				val packetParts = try {
					collectPart(multiValueMap, PACKET_PART_KEY, SharedConstants.MAX_PACKET_SIZE).awaitSingle()
				} catch (error: EmptyPacket) {
					logger.error("Packet part is empty")
					return@mono formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.RequestPartIsEmpty))
				} catch (error: RequestSizeExceeded) {
					logger.error("Packet part's size exceeds maxSize, requestSize = ${error.requestSize}")
					return@mono formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.ExceededMaxPacketSize))
				}

				val packet = jsonConverter.fromJson<SendPhotoPacket>(packetParts)

				//TODO: move isPacketOk from commons project to backend
				if (!packet.isPacketOk()) {
					logger.error("One or more of the packet's fields are incorrect")
					return@mono formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.BadRequest))
				}

				if (!userInfoRepository.accountExists(packet.userId)) {
					logger.error("Account with userId ${packet.userId} does not exist!")
					return@mono formatResponse(HttpStatus.FORBIDDEN, UploadPhotoResponse.fail(ErrorCode.AccountNotFound))
				}

				//user should not be able to send photo without creating default account with firebase token first
        val token = userInfoRepository.getFirebaseToken(packet.userId)
        if (token.isEmpty()) {
          logger.error("User does not have firebase token yet!")
          return@mono formatResponse(HttpStatus.FORBIDDEN, UploadPhotoResponse.fail(ErrorCode.UserDoesNotHaveFirebaseToken))
        }

        val photoParts = try {
					collectPart(multiValueMap, PHOTO_PART_KEY, SharedConstants.MAX_PHOTO_SIZE).awaitSingle()
        } catch (error: EmptyPacket) {
					logger.error("Photo part is empty")
					return@mono formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.RequestPartIsEmpty))
				} catch (error: RequestSizeExceeded) {
					logger.error("Photo part's size exceeds maxSize, requestSize = ${error.requestSize}")
					return@mono formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ErrorCode.ExceededMaxPhotoSize))
				}

				val newUploadingPhoto = photoInfoRepo.save(
          packet.userId,
          packet.lon,
          packet.lat,
          packet.isPublic,
          TimeUtils.getTimeFast(),
          ipHash
        )

        if (newUploadingPhoto.isEmpty()) {
          logger.error("Could not save a photoInfo")
          return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.DatabaseError))
        }

				try {
          if (!staticMapDownloaderService.enqueue(newUploadingPhoto.photoId)) {
            logger.error("Could not enqueue photo in locationMapReceiverService")

            deletePhotoWithFile(newUploadingPhoto)
            return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.DatabaseError))
          }

          val tempFile = saveTempFile(photoParts, newUploadingPhoto)
          if (tempFile.isEmpty()) {
            logger.error("Could not save file to disk")

            deletePhotoWithFile(newUploadingPhoto)
            return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.ServerDiskError))
          }

          try {
            diskManipulationService.resizeAndSavePhotos(tempFile, newUploadingPhoto)
          } catch (error: Throwable) {
            logger.error("Unknown error", error)
            deletePhotoWithFile(newUploadingPhoto)

            return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.ServerResizeError))
          } finally {
            tempFile.deleteIfExists()
          }

          try {
            cleanupService.tryToStartCleaningRoutine()
          } catch (error: Throwable) {
            logger.error("Error while cleaning up (cleanDatabaseAndPhotos)", error)
          }

          val exchangedPhoto = try {
            photoInfoRepo.tryDoExchange(packet.userId, newUploadingPhoto)
          } catch (error: Throwable) {
            logger.error("Unknown error while trying to do photo exchange", error)

            deletePhotoWithFile(newUploadingPhoto)
            return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.DatabaseError))
          }

          logger.debug("Photo has been successfully uploaded")

          if (!exchangedPhoto.isEmpty()) {
            pushNotificationSenderService.enqueue(exchangedPhoto)
          }

          val response = UploadPhotoResponse.success(
            newUploadingPhoto.photoId,
            newUploadingPhoto.photoName,
            newUploadingPhoto.uploadedOn
          )

          return@mono formatResponse(HttpStatus.OK, response)
				} catch (error: Throwable) {
          deletePhotoWithFile(newUploadingPhoto)

          throw error
        }
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ErrorCode.UnknownError))
			}
		}.flatMap { it }
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

	private fun saveTempFile(photoChunks: MutableList<DataBuffer>, photoInfo: PhotoInfo): FileWrapper {
		val filePath = "${ServerSettings.FILE_DIR_PATH}\\${photoInfo.photoName}"
		val outFile = File(filePath)

		logger.debug("outFile = ${outFile.absolutePath}")

		try {
			diskManipulationService.copyDataBuffersToFile(photoChunks, outFile)
		} catch (e: IOException) {
			logger.error("Error while trying to save file to the disk", e)

			return FileWrapper()
		}

		return FileWrapper(outFile)
	}

	private suspend fun deletePhotoWithFile(photoInfo: PhotoInfo) {
		if (!photoInfoRepo.delete(photoInfo)) {
			logger.error("Could not deletePhotoWithFile photo ${photoInfo.photoName}")
			return
		}

		diskManipulationService.deleteAllPhotoFiles(photoInfo.photoName)
	}
}

















