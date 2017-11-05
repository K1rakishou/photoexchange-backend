package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.request.SendPhotoPacket
import com.kirakishou.photoexchange.model.response.StatusResponse
import com.kirakishou.photoexchange.service.JsonConverterService
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

class UploadPhotoHandler(private val jsonConverter: JsonConverterService) {

    fun handle(request: ServerRequest): Mono<ServerResponse> {
        return request.body(BodyExtractors.toMultipartData())
                .flatMap {
                    val filePart = it.getFirst("photo")
                    val packetPart = it.getFirst("packet")

                    checkNotNull(filePart)
                    checkNotNull(packetPart)

                    val filePartsMono = filePart!!.content()
                            .buffer()
                            .single()

                    val packetPartsMono = packetPart!!.content()
                            .buffer()
                            .single()

                    return@flatMap Mono.zip(filePartsMono, packetPartsMono)
                }
                .flatMap {
                    val file = it.t1
                    val packetRaw = it.t2
                    val packet = jsonConverter.fromJson<SendPhotoPacket>(packetRaw, SendPhotoPacket::class.java)

                    val responseJson = jsonConverter.toJson(StatusResponse(ServerErrorCode.OK.value))
                    return@flatMap ServerResponse.ok().body(Mono.just(responseJson))
                }
    }
}

















