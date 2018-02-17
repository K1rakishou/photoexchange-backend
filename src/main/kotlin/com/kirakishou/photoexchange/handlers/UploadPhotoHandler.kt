package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.exception.EmptyFile
import com.kirakishou.photoexchange.model.exception.EmptyPacket
import com.kirakishou.photoexchange.model.net.request.SendPhotoPacket
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.GeneratorServiceImpl
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.IOUtils
import com.kirakishou.photoexchange.util.ImageUtils
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class UploadPhotoHandler(
        private val jsonConverter: JsonConverterService,
        private val photoInfoRepo: PhotoInfoRepository,
        private val generator: GeneratorServiceImpl
) : WebHandler {

    private val logger = LoggerFactory.getLogger(UploadPhotoHandler::class.java)
    private val PACKET_PART_KEY = "packet"
    private val PHOTO_PART_KEY = "photo"
    private val MAX_PHOTO_SIZE = 10 * (1024 * 1024) //10 megabytes
    private var fileDirectoryPath = "D:\\projects\\data\\photos"
    private var lastTimeCheck = 0L
    private val ONE_HOUR = TimeUnit.HOURS.toMillis(1)
    private val SEVEN_DAYS = TimeUnit.DAYS.toMillis(7)
    private val photoSizes = arrayOf("_o", "_s")

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val result = async {
            logger.debug("New UploadPhoto request")

            try {
                if (!request.containsAllPathVars(PACKET_PART_KEY, PHOTO_PART_KEY)) {
                    logger.debug("Request does not contain one of the required path variables")
                    return@async formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ServerErrorCode.BAD_REQUEST))
                }

                val multiValueMap = request.body(BodyExtractors.toMultipartData()).awaitFirst()
                val packetParts = collectPacketParts(multiValueMap).awaitFirst()
                val photoParts = collectPhotoParts(multiValueMap).awaitFirst()

                val packet = jsonConverter.fromJson<SendPhotoPacket>(packetParts)
                if (!packet.isPacketOk()) {
                    logger.debug("One or more of the packet's fields are incorrect")
                    return@async formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ServerErrorCode.BAD_REQUEST))
                }

                if (!checkPhotoTotalSize(photoParts)) {
                    logger.debug("Bad photo size")
                    return@async formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ServerErrorCode.BAD_REQUEST))
                }

                val newUploadingPhoto = createPhotoInfo(packet)
                val result = photoInfoRepo.save(newUploadingPhoto)

                if (result.isEmpty()) {
                    logger.debug("Could not save a photoInfo")
                    return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.REPOSITORY_ERROR))
                }

                val tempFile = saveTempFile(photoParts, newUploadingPhoto)
                if (tempFile == null) {
                    logger.debug("Could not save file to disk")
                    return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.DISK_ERROR))
                }

                try {
                    //save resized (original) version of the image
                    ImageUtils.resizeAndSaveImageOnDisk(tempFile, Dimension(1536, 1536), "_o", fileDirectoryPath, newUploadingPhoto.photoName)

                    //save small version of the image
                    ImageUtils.resizeAndSaveImageOnDisk(tempFile, Dimension(512, 512), "_s", fileDirectoryPath, newUploadingPhoto.photoName)

                } catch (error: Throwable) {
                    photoInfoRepo.deleteUserById(newUploadingPhoto.whoUploaded)
                    return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.DISK_ERROR))
                } finally {
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                }

//                val lastUploadedPhoto = photoInfoRepo.findOldestUploadedPhoto(packet.userId, newUploadingPhoto.photoName)
//                if (!lastUploadedPhoto.isEmpty()) {
//                    val now = TimeUtils.getTimeFast()
//
//                    if (!photoInfoRepo.updateSetPhotoReceiver(
//                            lastUploadedPhoto.whoUploaded,
//                            lastUploadedPhoto.photoName,
//                            newUploadingPhoto.whoUploaded,
//                            now)) {
//
//                    }
//
//                    if (!photoInfoRepo.updateSetPhotoReceiver(
//                            newUploadingPhoto.whoUploaded,
//                            newUploadingPhoto.photoName,
//                            lastUploadedPhoto.whoUploaded,
//                            now)) {
//
//                    }
//                }

                try {
                    deleteOldPhotos()
                } catch (error: Throwable) {
                    logger.error("Error while cleaning up (cleanDatabaseAndPhotos)", error)
                }

                logger.debug("Photo has been successfully uploaded")
                return@async formatResponse(HttpStatus.OK, UploadPhotoResponse.success(newUploadingPhoto.photoName))

            } catch (error: Throwable) {
                logger.error("Unknown error", error)
                return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.UNKNOWN_ERROR))
            }
        }

        return result
                .asMono(CommonPool)
                .flatMap { it }
    }

    private suspend fun deleteOldPhotos() {
        val now = TimeUtils.getTimeFast()

        //execute every hour
        if (now - lastTimeCheck > ONE_HOUR) {
            logger.debug("Start cleanDatabaseAndPhotos routine")

            lastTimeCheck = now
            cleanDatabaseAndPhotos(now - SEVEN_DAYS)

            logger.debug("End cleanDatabaseAndPhotos routine")
        }
    }

    private suspend fun cleanDatabaseAndPhotos(time: Long) {
        val photosToDelete = photoInfoRepo.findOlderThan(time)
        if (photosToDelete.isEmpty()) {
            return
        }

        val ids = photosToDelete.map { it.photoId }
        val isOk = photoInfoRepo.deleteAll(ids)

        if (!isOk) {
            return
        }

        for (photo in photosToDelete) {
            for (size in photoSizes) {
                val filePath = "$fileDirectoryPath\\${photo.photoName}$size"
                val file = File(filePath)

                if (file.exists() && file.isFile) {
                    file.delete()
                }
            }
        }
    }

    private fun formatResponse(httpStatus: HttpStatus, response: UploadPhotoResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.status(httpStatus).body(Mono.just(photoAnswerJson))
    }

    private fun saveTempFile(photoChunks: MutableList<DataBuffer>, photoInfo: PhotoInfo): File? {
        val filePath = "$fileDirectoryPath\\${photoInfo.photoName}"
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

    private fun createPhotoInfo(packet: SendPhotoPacket): PhotoInfo {
        val newPhotoName = generator.generateNewPhotoName()
        return PhotoInfo.create(
                packet.userId,
                newPhotoName,
                packet.lon,
                packet.lat,
                TimeUtils.getTimeFast())
    }

    private fun collectPhotoParts(map: MultiValueMap<String, Part>): Mono<MutableList<DataBuffer>> {
        return map.getFirst(PHOTO_PART_KEY)!!
                .content()
                .doOnNext { dataBuffer ->
                    if (dataBuffer.readableByteCount() == 0) {
                        throw EmptyFile()
                    }
                }
                .buffer()
                .single()
    }

    private fun collectPacketParts(map: MultiValueMap<String, Part>): Mono<MutableList<DataBuffer>> {
        return map.getFirst(PACKET_PART_KEY)!!
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

















