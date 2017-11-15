package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.StatusResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.JsonConverterService
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class MarkPhotoAsReceivedHandler(
        private val jsonConverter: JsonConverterService,
        private val photoInfoRepo: PhotoInfoRepository
) : WebHandler {

    private val PHOTO_ID_PATH_VARIABLE = "photo_id"
    private val USER_ID_PATH_VARIABLE = "user_id"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        //TODO: check USER_ID_PATH_VARIABLE existence
        val photoIdString = request.pathVariable(PHOTO_ID_PATH_VARIABLE)
        val userId = request.pathVariable(USER_ID_PATH_VARIABLE)

        val photoId = try {
            photoIdString.toLong()
        } catch (e: NumberFormatException) {
            -1L
        }

        if (photoId == -1L) {
            return getBodyResponse(HttpStatus.BAD_REQUEST, StatusResponse.from(ServerErrorCode.BAD_PHOTO_ID))
        }

        val updateResultFlux = photoInfoRepo.updateSetPhotoSuccessfullyDelivered(photoId, userId)
                .flux()
                .share()

        val wasUpdatedMono = updateResultFlux
                .filter { wasUpdated -> wasUpdated }
                .flatMap {
                    getBodyResponse(HttpStatus.OK, StatusResponse.from(ServerErrorCode.OK))
                }

        val wasNotUpdatedMono = updateResultFlux
                .filter { wasUpdated -> !wasUpdated }
                .flatMap {
                    getBodyResponse(HttpStatus.INTERNAL_SERVER_ERROR, StatusResponse.from(ServerErrorCode.UNKNOWN_ERROR))
                }

        return Flux.merge(wasUpdatedMono, wasNotUpdatedMono)
                .single()
                .doOnNext {
                    println(it.toString())
                }
    }

    private fun getBodyResponse(httpStatus: HttpStatus, response: StatusResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.status(httpStatus).body(Mono.just(photoAnswerJson))
    }
}