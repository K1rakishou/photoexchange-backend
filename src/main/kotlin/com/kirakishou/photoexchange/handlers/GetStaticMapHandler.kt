package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import kotlinx.coroutines.experimental.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.File

class GetStaticMapHandler(
	jsonConverter: JsonConverterService,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(GetStaticMapHandler::class.java)
	private val readChuckSize = 16384
	private val PHOTO_NAME_PATH_VARIABLE = "photo_name"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			logger.debug("New GetStaticMap request")

			try {
				if (!request.containsAllPathVars(PHOTO_NAME_PATH_VARIABLE)) {
					logger.debug("Request does not contain photoName variable")
					return@mono ServerResponse.badRequest().build()
				}

				val photoName = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)

				val file = File("${ServerSettings.FILE_DIR_PATH}\\${photoName}_map")
				if (!file.exists()) {
					logger.debug("Photo $photoName not found on the disk")
					return@mono ServerResponse.notFound().build()
				}

				val photoStreamFlux = Flux.using({
					return@using file.inputStream()
				}, { inputStream ->
					return@using DataBufferUtils.read(inputStream,
						DefaultDataBufferFactory(false, readChuckSize), readChuckSize)
				}, { inputStream ->
					logger.debug("Successfully returned a static map image")
					inputStream.close()
				})

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