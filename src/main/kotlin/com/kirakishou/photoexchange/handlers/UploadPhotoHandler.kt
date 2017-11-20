package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.PhotoInfo
import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.exception.EmptyFile
import com.kirakishou.photoexchange.model.exception.EmptyPacket
import com.kirakishou.photoexchange.model.net.request.SendPhotoPacket
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.GeneratorServiceImpl
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.IOUtils
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
import java.io.File
import java.io.IOException

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
    private val ONE_HOUR = 1000 * 60 * 60

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val result = async {
            try {
                val now = TimeUtils.getTimeFast()
                if (now - lastTimeCheck > ONE_HOUR) {
                    logger.debug("Start cleanDatabaseAndPhotos routine")

                    lastTimeCheck = now
                    cleanDatabaseAndPhotos(now - ONE_HOUR)
                }

                val multiValueMap = request.body(BodyExtractors.toMultipartData()).awaitFirst()
                if (!checkMultiValueMapPart(multiValueMap, PACKET_PART_KEY)) {
                    logger.debug("multipart request does not contain \"packet\" part")
                    return@async formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ServerErrorCode.BAD_REQUEST))
                }

                if (!checkMultiValueMapPart(multiValueMap, PHOTO_PART_KEY)) {
                    logger.debug("multipart request does not contain \"photo\" part")
                    return@async formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ServerErrorCode.BAD_REQUEST))
                }

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

                val photoInfo = createPhotoInfo(packet)
                val result = photoInfoRepo.save(photoInfo)
                if (result.isEmpty()) {
                    logger.debug("Could not save a photoInfo")
                    return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.REPOSITORY_ERROR))
                }

                if (!writePhotoToDisk(photoParts, photoInfo)) {
                    logger.debug("Could not save file to the disk")
                    return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.DISK_ERROR))
                }

                return@async formatResponse(HttpStatus.OK, UploadPhotoResponse.success(photoInfo.photoName, ServerErrorCode.OK))

            } catch (error: Throwable) {
                logger.error("Unknown error", error)
                return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.UNKNOWN_ERROR))
            }
        }

        return result
                .asMono(CommonPool)
                .flatMap { it }
    }

    private suspend fun cleanDatabaseAndPhotos(time: Long) {
        val photosToDelete = photoInfoRepo.findOlderThan(time)
        val isOk = photoInfoRepo.deleteOlderThan(time)
        if (!isOk) {
            return
        }

        for (photo in photosToDelete) {
            val filePath = "$fileDirectoryPath\\${photo.photoName}"
            val file = File(filePath)

            if (file.exists() && file.isFile) {
                file.delete()
            }
        }
    }

    private fun formatResponse(httpStatus: HttpStatus, response: UploadPhotoResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.status(httpStatus).body(Mono.just(photoAnswerJson))
    }

    private fun writePhotoToDisk(photoChunks: MutableList<DataBuffer>, photoInfo: PhotoInfo): Boolean {
        val filePath = "$fileDirectoryPath\\${photoInfo.photoName}"
        val outFile = File(filePath)

        try {
            IOUtils.copyDataBuffersToFile(photoChunks, outFile)
        } catch (e: IOException) {
            logger.error("Error while trying to save file to the disk", e)

            return false
        }

        return true
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
        return PhotoInfo(
                -1L,
                packet.userId,
                newPhotoName,
                "",
                packet.lon,
                packet.lat,
                0L,
                0L,
                TimeUtils.getTimeFast()
        )
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

    private fun checkMultiValueMapPart(map: MultiValueMap<String, Part>, key: String): Boolean {
        if (!map.contains(key)) {
            return false
        }

        if (map.getFirst(key) == null) {
            return false
        }

        return true
    }
}

















