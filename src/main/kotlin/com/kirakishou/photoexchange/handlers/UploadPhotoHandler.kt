package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.config.ServerSettings.BIG_PHOTO_SIZE
import com.kirakishou.photoexchange.config.ServerSettings.BIG_PHOTO_SUFFIX
import com.kirakishou.photoexchange.config.ServerSettings.DELETE_PHOTOS_OLDER_THAN
import com.kirakishou.photoexchange.config.ServerSettings.MAX_PHOTO_SIZE
import com.kirakishou.photoexchange.config.ServerSettings.MEDIUM_PHOTO_SIZE
import com.kirakishou.photoexchange.config.ServerSettings.MEDIUM_PHOTO_SUFFIX
import com.kirakishou.photoexchange.config.ServerSettings.OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL
import com.kirakishou.photoexchange.config.ServerSettings.SMALL_PHOTO_SIZE
import com.kirakishou.photoexchange.config.ServerSettings.SMALL_PHOTO_SUFFIX
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllParts
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.exception.EmptyPacket
import com.kirakishou.photoexchange.model.net.request.SendPhotoPacket
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.StaticMapDownloaderService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import com.kirakishou.photoexchange.util.IOUtils
import com.kirakishou.photoexchange.util.ImageUtils
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.experimental.reactive.awaitSingle
import kotlinx.coroutines.experimental.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.awt.Dimension
import java.io.File
import java.io.IOException

class UploadPhotoHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository,
	private val staticMapDownloaderService: StaticMapDownloaderService,
	private val concurrentService: AbstractConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(UploadPhotoHandler::class.java)
	private val PACKET_PART_KEY = "packet"
	private val PHOTO_PART_KEY = "photo"
	private var lastTimeCheck = 0L
	private val MAX_PHOTOS_TO_DELETE_PER_RUN = 1000
	private val photoSizes = arrayOf(BIG_PHOTO_SUFFIX, MEDIUM_PHOTO_SUFFIX, SMALL_PHOTO_SUFFIX)

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			logger.debug("New UploadPhoto request")

			try {
				val multiValueMap = request.body(BodyExtractors.toMultipartData()).awaitSingle()
				if (!multiValueMap.containsAllParts(PACKET_PART_KEY, PHOTO_PART_KEY)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						UploadPhotoResponse.fail(ErrorCode.UploadPhotoErrors.BadRequest))
				}

				val packetParts = collectPart(multiValueMap, PACKET_PART_KEY).awaitSingle()
				val photoParts = collectPart(multiValueMap, PHOTO_PART_KEY).awaitSingle()

				val packet = jsonConverter.fromJson<SendPhotoPacket>(packetParts)
				if (!packet.isPacketOk()) {
					logger.debug("One or more of the packet's fields are incorrect")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						UploadPhotoResponse.fail(ErrorCode.UploadPhotoErrors.BadRequest))
				}

				if (!checkPhotoTotalSize(photoParts)) {
					logger.debug("Bad photo size")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						UploadPhotoResponse.fail(ErrorCode.UploadPhotoErrors.BadRequest))
				}

				val photoInfoName = photoInfoRepo.generatePhotoInfoName()
				val newUploadingPhoto = photoInfoRepo.save(createPhotoInfo(photoInfoName, packet))

				if (newUploadingPhoto.isEmpty()) {
					logger.debug("Could not save a photoInfo")
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.UploadPhotoErrors.DatabaseError))
				}

				if (!staticMapDownloaderService.enqueue(newUploadingPhoto.photoId)) {
					//TODO: probably should delete the photo from the DB here
					logger.error("Could not enqueue photo in locationMapReceiverService")
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.UploadPhotoErrors.DatabaseError))
				}

				val tempFile = saveTempFile(photoParts, newUploadingPhoto)
				if (tempFile == null) {
					logger.debug("Could not save file to disk")
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.UploadPhotoErrors.DatabaseError))
				}

				try {
					resizeAndSavePhotos(tempFile, newUploadingPhoto)
				} catch (error: Throwable) {
					logger.error("Unknown error", error)
					photoInfoRepo.delete(newUploadingPhoto.uploaderUserId, photoInfoName)
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.UploadPhotoErrors.DatabaseError))
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

				try {
					photoInfoRepo.tryDoExchange(newUploadingPhoto)
				} catch (error: Throwable) {
					logger.error("Unknown error while trying to do photo exchange", error)
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						UploadPhotoResponse.fail(ErrorCode.UploadPhotoErrors.DatabaseError))
				}

				logger.debug("Photo has been successfully uploaded")
				return@mono formatResponse(HttpStatus.OK,
					UploadPhotoResponse.success(newUploadingPhoto.photoId, newUploadingPhoto.photoName))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					UploadPhotoResponse.fail(ErrorCode.UploadPhotoErrors.UnknownError))
			}
		}.flatMap { it }
	}

	private fun resizeAndSavePhotos(tempFile: File, newUploadingPhoto: PhotoInfo) {
		//save resized (big) version of the image
		val bigDimension = Dimension(BIG_PHOTO_SIZE, BIG_PHOTO_SIZE)
		ImageUtils.resizeAndSaveImageOnDisk(tempFile, bigDimension, BIG_PHOTO_SUFFIX,
			ServerSettings.FILE_DIR_PATH, newUploadingPhoto.photoName)

		//save resized (medium) version of the image
		val mediumDimension = Dimension(MEDIUM_PHOTO_SIZE, MEDIUM_PHOTO_SIZE)
		ImageUtils.resizeAndSaveImageOnDisk(tempFile, mediumDimension, MEDIUM_PHOTO_SUFFIX,
			ServerSettings.FILE_DIR_PATH, newUploadingPhoto.photoName)

		//save resized (small) version of the image
		val smallDimension = Dimension(SMALL_PHOTO_SIZE, SMALL_PHOTO_SIZE)
		ImageUtils.resizeAndSaveImageOnDisk(tempFile, smallDimension, SMALL_PHOTO_SUFFIX,
			ServerSettings.FILE_DIR_PATH, newUploadingPhoto.photoName)
	}

	private suspend fun deleteOldPhotos() {
		val now = TimeUtils.getTimeFast()

		//execute every hour
		if (now - lastTimeCheck > OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL) {
			logger.debug("Start cleanDatabaseAndPhotos routine")

			lastTimeCheck = now
			cleanDatabaseAndPhotos(now - DELETE_PHOTOS_OLDER_THAN)

			logger.debug("End cleanDatabaseAndPhotos routine")
		}
	}

	private suspend fun cleanDatabaseAndPhotos(time: Long) {
		val photosToDelete = photoInfoRepo.findOlderThan(time, MAX_PHOTOS_TO_DELETE_PER_RUN)
		if (photosToDelete.isEmpty()) {
			return
		}

		val ids = photosToDelete.map { it.photoId }
		if (!photoInfoRepo.deleteAll(ids)) {
			return
		}

		for (photo in photosToDelete) {
			for (size in photoSizes) {
				val filePath = "${ServerSettings.FILE_DIR_PATH}\\${photo.photoName}$size"
				val file = File(filePath)

				if (file.exists() && file.isFile) {
					file.delete()
				}
			}
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

	private fun createPhotoInfo(photoName: String, packet: SendPhotoPacket): PhotoInfo {
		return PhotoInfo.create(
			packet.userId,
			photoName,
			packet.isPublic,
			packet.lon,
			packet.lat,
			TimeUtils.getTimeFast())
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

















