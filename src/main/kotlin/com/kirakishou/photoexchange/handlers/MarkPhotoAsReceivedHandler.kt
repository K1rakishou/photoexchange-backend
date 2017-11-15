package com.kirakishou.photoexchange.handlers

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
            //TODO: return bad photoId errorCode
        }

        val updateResultFlux = photoInfoRepo.updateSetPhotoSuccessfullyDelivered(photoId, userId)
                .flux()
                .share()

        val wasUpdatedMono = updateResultFlux
                .filter { wasUpdated -> wasUpdated }
                .flatMap { ServerResponse.ok().body(Mono.just("ok")) }

        val wasNotUpdatedMono = updateResultFlux
                .filter { wasUpdated -> !wasUpdated }
                .flatMap { ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Mono.just("not ok")) }

        return Flux.merge(wasUpdatedMono, wasNotUpdatedMono)
                .single()
    }
}