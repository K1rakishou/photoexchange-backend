package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings.FILE_DIR_PATH
import com.kirakishou.photoexchange.config.ServerSettings.PHOTO_SIZES
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.service.JsonConverterService
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
	jsonConverter: JsonConverterService
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(GetPhotoHandler::class.java)
	private val readChuckSize = 16384
	private val PHOTO_NAME_PATH_VARIABLE = "photo_name"
	private val PHOTO_SIZE_PATH_VARIABLE = "photo_size"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono {
			logger.debug("New GetPhoto request")

			try {
				if (!request.containsAllPathVars(PHOTO_NAME_PATH_VARIABLE, PHOTO_SIZE_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono ServerResponse.badRequest().build()
				}

				val photoName = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)
				val photoSize = request.pathVariable(PHOTO_SIZE_PATH_VARIABLE)

				if (!PHOTO_SIZES.contains(photoSize)) {
					logger.debug("Photo size $photoSize param is neither of $PHOTO_SIZES")
					return@mono ServerResponse.badRequest().build()
				}

				val file = File("$FILE_DIR_PATH\\${photoName}_$photoSize")
				if (!file.exists()) {
					logger.debug("Photo $photoName not found on the disk")
					return@mono ServerResponse.notFound().build()
				}

				//TODO: may not work
				val photoStreamFlux = DataBufferUtils.read(FileSystemResource(file),
					DefaultDataBufferFactory(false, readChuckSize), readChuckSize)

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