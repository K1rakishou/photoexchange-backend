package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerJsonObject
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.JsonConverterService
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GetPhotoAnswerHandler(
        private val jsonConverter: JsonConverterService,
        private val photoInfoRepo: PhotoInfoRepository
) : WebHandler {

    private val USER_ID_PATH_VARIABLE = "user_id"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        //TODO: check USER_ID_PATH_VARIABLE existence
        val userId = request.pathVariable(USER_ID_PATH_VARIABLE)

        val countFlux = photoInfoRepo.countUserUploadedPhotos(userId)
                .map { it.toInt() }
                .flux()
                .share()

        val userHasNoUploadedPhotosFlux = countFlux
                .filter { count -> count <= 0 }
                .flatMap { getBodyResponse(PhotoAnswerResponse.fail(ServerErrorCode.USER_HAS_NO_UPLOADED_PHOTOS)) }

        val userHasUploadedPhotosFlux = countFlux
                .filter { count -> count > 0 }
                .flatMap { photoInfoRepo.findPhotoInfo(userId) }
                .zipWith(countFlux)
                .flatMap {
                    val photoInfo = it.t1
                    val count = it.t2

                    if (photoInfo.isEmpty()) {
                        return@flatMap getBodyResponse(PhotoAnswerResponse.fail(ServerErrorCode.NO_PHOTOS_TO_SEND_BACK))
                    }

                    val photoAnswer = PhotoAnswerJsonObject(
                            photoInfo.photoId,
                            photoInfo.whoUploaded,
                            photoInfo.photoName,
                            photoInfo.lon,
                            photoInfo.lat)

                    val allFound = (count - 1) > 0
                    return@flatMap getBodyResponse(PhotoAnswerResponse.success(photoAnswer, allFound, ServerErrorCode.OK))
                }

        return Flux.merge(userHasNoUploadedPhotosFlux, userHasUploadedPhotosFlux)
                .single()
    }

    private fun getBodyResponse(response: PhotoAnswerResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.ok().body(Mono.just(photoAnswerJson))
    }
}