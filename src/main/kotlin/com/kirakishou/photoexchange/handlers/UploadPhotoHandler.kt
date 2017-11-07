package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.PhotoInfo
import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.exception.*
import com.kirakishou.photoexchange.model.net.request.SendPhotoPacket
import com.kirakishou.photoexchange.model.net.response.StatusResponse
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.GeneratorServiceImpl
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.IOUtils
import com.kirakishou.photoexchange.util.TimeUtils
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
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
        val multiValueMapMono = request.body(BodyExtractors.toMultipartData())
                .flux()
                .share()

        val packetRawMono = multiValueMapMono
                .doOnNext { map -> checkMultiValueMapPart(map, PACKET_PART_KEY) }
                .flatMap(this::collectPacketParts)
                .single()

        val photoPartMono = multiValueMapMono
                .doOnNext { map -> checkMultiValueMapPart(map, PHOTO_PART_KEY) }
                .flatMap(this::collectPhotoParts)
                .single()

        val photoInfoMono = packetRawMono
                .map { packetRaw -> jsonConverter.fromJson<SendPhotoPacket>(packetRaw, SendPhotoPacket::class.java) }
                .doOnSuccess(this::checkPacketCorrectness)
                .map(this::createPhotoInfo)
                .flatMap { storedPhoto -> photoInfoRepo.save(storedPhoto) }
                .doOnSuccess(this::checkRepoResult)

        return photoPartMono
                .doOnSuccess(this::checkPhotoTotalSize)
                .zipWith(photoInfoMono)
                .doOnSuccess(this::writePhotoToDisk)
                .map { it.t2.photoName }
                .flatMap(this::sendResponse)
                .onErrorResume(this::handleErrors)
    }

    private fun handleErrors(error: Throwable): Mono<ServerResponse> {
        return when (error) {
            is MultiValueMapDoesNotContainsPart ->
                formatResponse(HttpStatus.BAD_REQUEST, ServerErrorCode.BAD_REQUEST)
            is MultiPartRequestPartIsNull ->
                formatResponse(HttpStatus.BAD_REQUEST, ServerErrorCode.BAD_REQUEST)
            is EmptyPacket ->
                formatResponse(HttpStatus.BAD_REQUEST, ServerErrorCode.BAD_REQUEST)
            is EmptyFile ->
                formatResponse(HttpStatus.BAD_REQUEST, ServerErrorCode.BAD_REQUEST)
            is PacketContainsBadData ->
                formatResponse(HttpStatus.BAD_REQUEST, ServerErrorCode.BAD_REQUEST)
            is PhotoSizeExceeded ->
                formatResponse(HttpStatus.BAD_REQUEST, ServerErrorCode.BAD_REQUEST)
            is CouldNotSavePhotoToDb ->
                formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, ServerErrorCode.REPOSITORY_ERROR)
            is ErrorWhileWritingPhotoToDisk ->
                formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, ServerErrorCode.DISK_ERROR)

            else -> {
                error.printStackTrace()
                formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, ServerErrorCode.UNKNOWN_ERROR)
            }
        }
    }

    private fun formatResponse(httpStatus: HttpStatus, errorCode: ServerErrorCode): Mono<ServerResponse> {
        return ServerResponse.status(httpStatus)
                .body(Mono.just(StatusResponse(errorCode.value)))
    }

    private fun sendResponse(photoName: String): Mono<ServerResponse> {
        val response = UploadPhotoResponse(photoName, ServerErrorCode.OK)
        val responseJson = jsonConverter.toJson(response)

        return ServerResponse.ok().body(Mono.just(responseJson))
    }

    private fun writePhotoToDisk(it: Tuple2<MutableList<DataBuffer>, PhotoInfo>) {
        val photoChunks = it.t1
        val photoInfo = it.t2

        val filePath = "$fileDirectoryPath\\${photoInfo.photoName}"
        val outFile = File(filePath)

        try {
            IOUtils.copyDataBuffersToFile(photoChunks, outFile)
        } catch (e: IOException) {
            e.printStackTrace()
            photoInfoRepo.deleteById(photoInfo.whoUploaded)

            throw ErrorWhileWritingPhotoToDisk()
        }
    }

    private fun checkPhotoTotalSize(photo: MutableList<DataBuffer>) {
        val totalLength = photo.sumBy { it.readableByteCount() }
        if (totalLength > MAX_PHOTO_SIZE) {
            throw PhotoSizeExceeded()
        }
    }

    private fun checkRepoResult(result: PhotoInfo) {
        if (result.isEmpty()) {
            throw CouldNotSavePhotoToDb()
        }
    }

    private fun checkPacketCorrectness(packet: SendPhotoPacket) {
        if (!packet.isPacketOk()) {
            throw PacketContainsBadData()
        }
    }

    private fun createPhotoInfo(packet: SendPhotoPacket): PhotoInfo {
        val newPhotoName = generator.generateNewPhotoName()
        return PhotoInfo(
                -1L,
                packet.userId,
                newPhotoName,
                packet.lon,
                packet.lat,
                false,
                false,
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

    private fun checkMultiValueMapPart(map: MultiValueMap<String, Part>, key: String) {
        if (!map.contains(key)) {
            throw MultiValueMapDoesNotContainsPart()
        }

        if (map.getFirst(key) == null) {
            throw MultiPartRequestPartIsNull()
        }
    }
}

















