package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.GetUserLocationResponse
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

class GetUserLocation(
    private val jsonConverter: JsonConverterService,
    private val photoInfoRepo: PhotoInfoRepository
) : WebHandler {
    private val logger = LoggerFactory.getLogger(UploadPhotoHandler::class.java)
    private val USER_ID_PATH_VARIABLE = "user_id"
    private val PHOTO_ID_PATH_VARIABLE = "photo_id"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val result = async {
            logger.debug("New GetUserLocation request")

            try {
                val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
                val photoId = request.pathVariable(PHOTO_ID_PATH_VARIABLE)

                val photoInfo = photoInfoRepo.findUploadedPhotoNewLocation(userId, photoId)
                if (photoInfo.isEmpty()) {
                    return@async formatResponse(HttpStatus.NOT_FOUND, GetUserLocationResponse.fail(ServerErrorCode.NOT_FOUND))
                }

                return@async formatResponse(HttpStatus.OK, GetUserLocationResponse.success(photoInfo.lat, photoInfo.lon))
            } catch (error: Throwable) {
                logger.error("Unknown error", error)
                return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, GetUserLocationResponse.fail(ServerErrorCode.UNKNOWN_ERROR))
            }
        }

        return result
                .asMono(CommonPool)
                .flatMap { it }
    }

    private fun formatResponse(httpStatus: HttpStatus, response: GetUserLocationResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.status(httpStatus).body(Mono.just(photoAnswerJson))
    }
}