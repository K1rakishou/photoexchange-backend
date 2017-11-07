package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.net.request.GetPhotoAnswerPacket
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

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(GetPhotoAnswerPacket::class.java)
                .flatMap {
                    println(it.userId)
                    ServerResponse.ok().body(Mono.just("123"))
                }
    }
}