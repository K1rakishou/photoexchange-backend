package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import core.SharedConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import java.io.File

class GetStaticMapHandler(
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetStaticMapHandler::class.java)
  private val readChuckSize = 16384

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetStaticMap request")

      try {
        val photoName = request.getStringVariable(
          Router.PHOTO_NAME_VARIABLE,
          SharedConstants.MAX_PHOTO_NAME_LEN
        )

        if (photoName == null) {
          logger.debug("Bad param photoName ($photoName)")
          return@mono ServerResponse.badRequest().build()
        }

        val file = File("${ServerSettings.FILE_DIR_PATH}\\${photoName}_map")
        if (!file.exists()) {
          logger.debug("Static map $photoName not found on the disk")
          return@mono ServerResponse.notFound().build()
        }

        val photoStreamFlux = DataBufferUtils.read(
          FileSystemResource(file),
          DefaultDataBufferFactory(false, readChuckSize), readChuckSize
        )

        return@mono ServerResponse.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=$photoName")
          .body(photoStreamFlux)

      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .build()
      }
    }.flatMap { it }
  }
}