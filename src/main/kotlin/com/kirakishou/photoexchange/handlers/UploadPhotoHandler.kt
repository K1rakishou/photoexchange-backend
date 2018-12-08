package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.config.ServerSettings.MAX_PHOTO_SIZE
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllParts
import com.kirakishou.photoexchange.exception.EmptyPacket
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.database.repository.BanListRepository
import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.PushNotificationSenderService
import com.kirakishou.photoexchange.service.RemoteAddressExtractorService
import com.kirakishou.photoexchange.service.StaticMapDownloaderService
import com.kirakishou.photoexchange.util.IOUtils
import com.kirakishou.photoexchange.service.ImageManipulationService
import com.kirakishou.photoexchange.util.SecurityUtils
import com.kirakishou.photoexchange.util.TimeUtils
import core.ErrorCode
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

class UploadPhotoHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository,
  private val userInfoRepository: UserInfoRepository,
  private val banListRepository: BanListRepository,
	private val staticMapDownloaderService: StaticMapDownloaderService,
  private val pushNotificationSenderService: PushNotificationSenderService,
  private val remoteAddressExtractorService: RemoteAddressExtractorService,
	private val imageManipulationService: ImageManipulationService
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(UploadPhotoHandler::class.java)
	private val PACKET_PART_KEY = "packet"
	private val PHOTO_PART_KEY = "photo"
	private val mutex = Mutex()
	private var lastTimeCheck = 0L

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono {
			logger.debug("New UploadPhoto request")

			try {
        val ipHash = getIpAddressHash(remoteAddressExtractorService.extractRemoteAddress(request))
        if (banListRepository.isBanned(ipHash)) {
          logger.error("User is banned. ipHash = $ipHash")
          return@mono formatResponse(HttpStatus.FORBIDDEN,
            UploadPhotoResponse.fail(ErrorCode.YouAreBanned))
        }

				val multiValueMap = request.body(BodyExtractors.toMultipartData()).awaitSingle()
				if (!multiValueMap.containsAllParts(PACKET_PART_KEY, PHOTO_PART_KEY)) {
					logger.error("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						UploadPhotoResponse.fail(ErrorCode.BadRequest))
				}

				val packetParts = collectPart(multiValueMap, PACKET_PART_KEY).awaitSingle()
				val packet = jsonConverter.fromJson<SendPhotoPacket>(packetParts)
				if (!packet.isPacketOk()) {
					logger.error("One or more of the packet's fields are incorrect")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						UploadPhotoResponse.fail(ErrorCode.BadRequest))
				}

        //user should not be able to send photo without creating default account with firebase token first
        val token = userInfoRepository.getFirebaseToken(packet.userId)
        if (token.isEmpty()) {
          logger.error("User does not have firebase token yet!")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            UploadPhotoResponse.fail(ErrorCode.UserDoesNotHaveFirebaseToken))
        }

        val photoParts = collectPart(multiValueMap, PHOTO_PART_KEY).awaitSingle()
        if (!checkPhotoTotalSize(photoParts)) {
					logger.error("Bad photo size")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						UploadPhotoResponse.fail(ErrorCode.ExceededMaxPhotoSize))
				}

				val photoInfoName = photoInfoRepo.generatePhotoInfoName()
				val newUploadingPhoto = photoInfoRepo.save(createPhotoInfo(photoInfoName, packet, ipHash))

				if (newUploadingPhoto.isEmpty()) {
					logger.error("Could not save a photoInfo")
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.DatabaseError))
				}

				if (!staticMapDownloaderService.enqueue(newUploadingPhoto.photoId)) {
					logger.error("Could not enqueue photo in locationMapReceiverService")

					cleanup(newUploadingPhoto)
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.DatabaseError))
				}

				val tempFile = saveTempFile(photoParts, newUploadingPhoto)
				if (tempFile == null) {
					logger.error("Could not save file to disk")

					cleanup(newUploadingPhoto)
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.ServerDiskError))
				}

				try {
					imageManipulationService.resizeAndSavePhotos(tempFile, newUploadingPhoto)
				} catch (error: Throwable) {
					logger.error("Unknown error", error)
					photoInfoRepo.delete(newUploadingPhoto.userId, photoInfoName)
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.ServerResizeError))
				} finally {
					if (tempFile.exists()) {
						tempFile.delete()
					}
				}

				try {
					deleteOldPhotos()
				} catch (error: Throwable) {
					logger.error("Error while cleaning up (cleanDatabaseAndPhotos)", error)
				}

				val exchangedPhoto = try {
					photoInfoRepo.tryDoExchange(packet.userId, newUploadingPhoto)
				} catch (error: Throwable) {
					logger.error("Unknown error while trying to do photo exchange", error)

					cleanup(newUploadingPhoto)
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.DatabaseError))
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
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					UploadPhotoResponse.fail(ErrorCode.UnknownError))
			}
		}.flatMap { it }
	}

  private fun getIpAddressHash(ipAddress: String): String {
    val hash = SecurityUtils.Hashing.sha3(ipAddress)

    logger.debug("ip = $ipAddress, hash = $hash")
    return hash
  }

  private suspend fun cleanup(photoInfo: PhotoInfo) {
		photoInfoRepo.deleteAll(listOf(photoInfo.photoId))

		val photoPath = "${ServerSettings.FILE_DIR_PATH}\\${photoInfo.photoName}"
		IOUtils.deleteAllPhotoFiles(photoPath)
	}

	//TODO: move to repository
	private suspend fun deleteOldPhotos() {
		mutex.withLock {
			val now = TimeUtils.getTimeFast()

			//execute every hour
			if (now - lastTimeCheck > ServerSettings.OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL) {
				logger.debug("Start cleanDatabaseAndPhotos routine")

				lastTimeCheck = now
				cleanDatabaseAndPhotos(now - ServerSettings.DELETE_PHOTOS_OLDER_THAN)

				logger.debug("End cleanDatabaseAndPhotos routine")
			}
		}
	}

	//TODO: move to repository?
	//TODO: make deletion in two passes
	//1. First pass should mark old photos with deleteOn = currentTime()
	//2. Second pass should find all photos with deletedOn that already marked more than n days
	private suspend fun cleanDatabaseAndPhotos(time: Long) {
		val photosToDelete = photoInfoRepo.findOlderThan(time)
		if (photosToDelete.isEmpty()) {
			return
		}

		val ids = photosToDelete.map { it.photoId }
		if (!photoInfoRepo.deleteAll(ids)) {
			return
		}

		logger.debug("Found ${ids.size} photo ids to delete")

		for (photo in photosToDelete) {
			cleanup(photo)
		}
	}

	private fun saveTempFile(photoChunks: MutableList<DataBuffer>, photoInfo: PhotoInfo): File? {
		val filePath = "${ServerSettings.FILE_DIR_PATH}\\${photoInfo.photoName}"
		val outFile = File(filePath)

		try {
			IOUtils.copyDataBuffersToFile(photoChunks, outFile)
		} catch (e: IOException) {
			logger.error("Error while trying to save file to the disk", e)

			return null
		}

		return outFile
	}

	private fun checkPhotoTotalSize(photo: MutableList<DataBuffer>): Boolean {
		val totalLength = photo.sumBy { it.readableByteCount() }
		if (totalLength > MAX_PHOTO_SIZE) {
			return false
		}

		return true
	}

	private fun createPhotoInfo(photoName: String, packet: SendPhotoPacket, ipHash: String): PhotoInfo {
		return PhotoInfo.create(
			packet.userId,
			photoName,
			packet.isPublic,
			packet.lon,
			packet.lat,
			TimeUtils.getTimeFast(),
      ipHash
    )
	}

	private fun collectPart(map: MultiValueMap<String, Part>, partName: String): Mono<MutableList<DataBuffer>> {
		return map.getFirst(partName)!!
			.content()
			.doOnNext { dataBuffer ->
				if (dataBuffer.readableByteCount() == 0) {
					throw EmptyPacket()
				}
			}
			.buffer()
			.single()
	}
}

















