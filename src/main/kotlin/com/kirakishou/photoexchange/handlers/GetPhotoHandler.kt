package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings.FILE_DIR_PATH
import com.kirakishou.photoexchange.config.ServerSettings.PHOTO_SIZES
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

class GetPhotoHandler(
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetPhotoHandler::class.java)
  private val readChuckSize = 16384

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetPhoto request")

      try {
        val photoName = request.getStringVariable(
          Router.PHOTO_NAME_VARIABLE,
          SharedConstants.MAX_PHOTO_NAME_LEN
        )

        if (photoName == null) {
          logger.debug("Bad param photoName ($photoName)")
          return@mono ServerResponse.badRequest().build()
        }

        val photoSize = request.getStringVariable(
          Router.PHOTO_SIZE_VARIABLE,
          2
        )

        if (photoSize == null) {
          logger.debug("Bad param photoSize ($photoSize)")
          return@mono ServerResponse.badRequest().build()
        }

        if (!PHOTO_SIZES.contains(photoSize)) {
          logger.debug("Photo size $photoSize param is neither of $PHOTO_SIZES")
          return@mono ServerResponse.badRequest().build()
        }

        val file = File("$FILE_DIR_PATH\\${photoName}_$photoSize")
        if (!file.exists()) {
          logger.debug("Photo $photoName not found on the disk")
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