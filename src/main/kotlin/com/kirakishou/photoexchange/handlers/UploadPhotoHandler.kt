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

    private val PACKET_PART_KEY = "packet"
    private val PHOTO_PART_KEY = "photo"
    private val MAX_PHOTO_SIZE = 10 * (1024 * 1024) //10 megabytes
    private var fileDirectoryPath = "D:\\projects\\data\\photos"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val result = async {
            val multiValueMap = request.body(BodyExtractors.toMultipartData()).awaitFirst()
            if (!checkMultiValueMapPart(multiValueMap, PACKET_PART_KEY)) {
                return@async formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ServerErrorCode.BAD_REQUEST))
            }

            if (!checkMultiValueMapPart(multiValueMap, PHOTO_PART_KEY)) {
                return@async formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ServerErrorCode.BAD_REQUEST))
            }

            val packetParts = collectPacketParts(multiValueMap).awaitFirst()
            val photoParts = collectPhotoParts(multiValueMap).awaitFirst()

            val packet = jsonConverter.fromJson<SendPhotoPacket>(packetParts)
            if (!packet.isPacketOk()) {
                return@async formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ServerErrorCode.BAD_REQUEST))
            }

            val photoInfo = createPhotoInfo(packet)
            val result = photoInfoRepo.save(photoInfo)
            if (result.isEmpty()) {
                return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.REPOSITORY_ERROR))
            }

            if (!checkPhotoTotalSize(photoParts)) {
                return@async formatResponse(HttpStatus.BAD_REQUEST, UploadPhotoResponse.fail(ServerErrorCode.BAD_REQUEST))
            }

            if (!writePhotoToDisk(photoParts, photoInfo)) {
                return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.DISK_ERROR))
            }

            return@async formatResponse(HttpStatus.OK, UploadPhotoResponse.success(photoInfo.photoName, ServerErrorCode.OK))
        }

        return result
                .asMono(CommonPool)
                .flatMap { it }
    }

    private fun formatResponse(httpStatus: HttpStatus, response: UploadPhotoResponse): Mono<ServerResponse> {
        return ServerResponse.status(httpStatus)
                .body(Mono.just(response))
    }

    private suspend fun writePhotoToDisk(photoChunks: MutableList<DataBuffer>, photoInfo: PhotoInfo): Boolean {
        val filePath = "$fileDirectoryPath\\${photoInfo.photoName}"
        val outFile = File(filePath)

        try {
            IOUtils.copyDataBuffersToFile(photoChunks, outFile)
        } catch (e: IOException) {
            e.printStackTrace()

            val deleteResult = photoInfoRepo.deleteById(photoInfo.whoUploaded)
            if (!deleteResult) {
                System.err.println("Could not delete photo info")
            }

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
                TimeUtils.getTime()
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

















