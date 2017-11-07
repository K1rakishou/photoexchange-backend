package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.JsonConverterService
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

class GetPhotoAnswerHandler(
        private val jsonConverter: JsonConverterService,
        private val photoInfoRepo: PhotoInfoRepository
) : WebHandler {

    private val USER_ID_PATH_VARIABLE = "user_id"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val userIdMono = Mono.just(request.pathVariable(USER_ID_PATH_VARIABLE))

        return userIdMono.flatMap { userId -> photoInfoRepo.findPhotoInfo(userId) }
                .flatMap { photoInfo ->
                    val photoAnswer = PhotoAnswerResponse(photoInfo.whoUploaded, photoInfo.photoName, ServerErrorCode.OK)
                    val photoAnswerJson = jsonConverter.toJson(photoAnswer)

                    return@flatMap ServerResponse.ok().body(Mono.just(photoAnswerJson))
                }
    }
}