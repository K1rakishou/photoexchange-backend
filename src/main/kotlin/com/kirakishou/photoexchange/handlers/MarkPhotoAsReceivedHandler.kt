package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.StatusResponse
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

class MarkPhotoAsReceivedHandler(
        private val jsonConverter: JsonConverterService,
        private val photoInfoRepo: PhotoInfoRepository
) : WebHandler {
    private val logger = LoggerFactory.getLogger(MarkPhotoAsReceivedHandler::class.java)
    private val PHOTO_ID_PATH_VARIABLE = "photo_id"
    private val USER_ID_PATH_VARIABLE = "user_id"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val result = async {
            try {
                val pathVariables = request.pathVariables()
                if (!pathVariables.containsKey(PHOTO_ID_PATH_VARIABLE)) {
                    logger.debug("request does not contain photo_id variable")
                    return@async formatResponse(HttpStatus.BAD_REQUEST, StatusResponse.from(ServerErrorCode.BAD_REQUEST))
                }

                if (!pathVariables.containsKey(USER_ID_PATH_VARIABLE)) {
                    logger.debug("request does not contain user_id variable")
                    return@async formatResponse(HttpStatus.BAD_REQUEST, StatusResponse.from(ServerErrorCode.BAD_REQUEST))
                }

                val photoIdString =  request.pathVariable(PHOTO_ID_PATH_VARIABLE)
                val userId = request.pathVariable(USER_ID_PATH_VARIABLE)

                val photoId = try {
                    photoIdString.toLong()
                } catch (e: NumberFormatException) {
                    -1L
                }

                if (photoId == -1L) {
                    logger.debug("Couldn't convert photoId string to long")
                    return@async formatResponse(HttpStatus.BAD_REQUEST, StatusResponse.from(ServerErrorCode.BAD_PHOTO_ID))
                }

                if (!photoInfoRepo.updateSetPhotoSuccessfullyDelivered(photoId, userId)) {
                    logger.debug("Couldn't update photo delivered")
                    return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, StatusResponse.from(ServerErrorCode.UNKNOWN_ERROR))
                }

                return@async formatResponse(HttpStatus.OK, StatusResponse.from(ServerErrorCode.OK))

            } catch (error: Throwable) {
                logger.error("Unknown error", error)
                return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, UploadPhotoResponse.fail(ServerErrorCode.UNKNOWN_ERROR))
            }
        }

        return result
                .asMono(CommonPool)
                .flatMap { it }
    }

    private fun formatResponse(httpStatus: HttpStatus, response: StatusResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.status(httpStatus).body(Mono.just(photoAnswerJson))
    }
}