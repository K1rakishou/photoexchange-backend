package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GetGalleryPhotosResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetGalleryPhotosHandler(
	jsonConverter: JsonConverterService,
	private val galleryPhotosRepository: GalleryPhotosRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetGalleryPhotosHandler::class.java)
	private val LAST_ID_VARIABLE = "last_id"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		val result = concurrentService.asyncCommon {
			logger.debug("New GetGalleryPhotos request")

			if (!request.containsAllPathVars(LAST_ID_VARIABLE)) {
				logger.debug("Request does not contain one of the required path variables")
				return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
					GetGalleryPhotosResponse.fail(ErrorCode.GetGalleryPhotosErrors.BadRequest()))
			}

			val lastId = try {
				request.pathVariable(LAST_ID_VARIABLE).toLong()
			} catch (error: NumberFormatException) {
				Long.MAX_VALUE
			}

			logger.debug("LastId: $lastId")

			val galleryPhotos = galleryPhotosRepository.findPaged(lastId)
			galleryPhotos.forEach { logger.debug("id = ${it.photoId}, uploadedOn = ${it.uploadedOn}") }

			return@asyncCommon formatResponse(HttpStatus.OK, GetGalleryPhotosResponse.success())
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}
}