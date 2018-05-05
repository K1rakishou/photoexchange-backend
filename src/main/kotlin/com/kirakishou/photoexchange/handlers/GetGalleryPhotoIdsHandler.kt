package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GalleryPhotoIdsResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetGalleryPhotoIdsHandler(
	jsonConverter: JsonConverterService,
	private val galleryPhotosRepository: GalleryPhotosRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetGalleryPhotoIdsHandler::class.java)
	private val MAX_PHOTOS_PER_REQUEST_COUNT = 100
	private val LAST_ID_VARIABLE = "last_id"
	private val COUNT_VARIABLE = "count"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		val result = concurrentService.asyncCommon {
			try {
				logger.debug("New GetGalleryPhotoIds request")

				if (!request.containsAllPathVars(LAST_ID_VARIABLE, COUNT_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotoIdsResponse.fail(ErrorCode.GalleryPhotoIdsErrors.BadRequest()))
				}

				val lastId = try {
					request.pathVariable(LAST_ID_VARIABLE).toLong()
				} catch (error: NumberFormatException) {
					Long.MAX_VALUE
				}

				val count = try {
					request.pathVariable(COUNT_VARIABLE).toInt().coerceAtMost(MAX_PHOTOS_PER_REQUEST_COUNT)
				} catch (error: NumberFormatException) {
					MAX_PHOTOS_PER_REQUEST_COUNT
				}

				val galleryPhotoIds = galleryPhotosRepository.findPaged(lastId, count)

				logger.debug("Found ${galleryPhotoIds.size} photo ids from gallery")
				return@asyncCommon formatResponse(HttpStatus.OK,
					GalleryPhotoIdsResponse.success(galleryPhotoIds))

			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@asyncCommon formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GalleryPhotoIdsResponse.fail(ErrorCode.GalleryPhotoIdsErrors.UnknownError()))
			}
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}
}