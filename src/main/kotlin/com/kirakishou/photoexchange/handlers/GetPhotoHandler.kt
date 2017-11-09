package com.kirakishou.photoexchange.handlers

import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.File

class GetPhotoHandler : WebHandler {

    private val readChuckSize = 16384
    private val PHOTO_NAME_PATH_VARIABLE = "photo_name"
    private var fileDirectoryPath = "D:\\projects\\data\\photos"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val photoName = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)

        val photoStreamFlux = Flux.using({
            val path = "$fileDirectoryPath\\$photoName"
            return@using File(path).inputStream()
        }, { inputStream ->
            return@using DataBufferUtils.read(inputStream, DefaultDataBufferFactory(false, readChuckSize), readChuckSize)
        }, { inputStream ->
            inputStream.close()
        })

        return ServerResponse.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=photo")
                .body(photoStreamFlux)
    }
}