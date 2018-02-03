package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.extensions.containsAllPathVars
import org.slf4j.LoggerFactory
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
    private val logger = LoggerFactory.getLogger(GetPhotoHandler::class.java)
    private val readChuckSize = 16384
    private val PHOTO_NAME_PATH_VARIABLE = "photo_name"
    private val PHOTO_SIZE_PATH_VARIABLE = "photo_size"
    private var fileDirectoryPath = "D:\\projects\\data\\photos"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        logger.debug("GetPhoto request")

        if (!request.containsAllPathVars(PHOTO_NAME_PATH_VARIABLE, PHOTO_SIZE_PATH_VARIABLE)) {
            logger.debug("Request does not contain one of the required path variables")
            return ServerResponse.badRequest().build()
        }

        val photoName = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)
        val photoSize = request.pathVariable(PHOTO_SIZE_PATH_VARIABLE)

        if (photoSize != "o" && photoSize != "s") {
            return ServerResponse.notFound().build()
        }

        val photoStreamFlux = Flux.using({
            val path = "$fileDirectoryPath\\${photoName}_${photoSize}"
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