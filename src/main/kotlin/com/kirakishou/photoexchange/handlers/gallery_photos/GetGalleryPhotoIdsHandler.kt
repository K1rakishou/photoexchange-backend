package com.kirakishou.photoexchange.handlers.gallery_photos

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.AbstractWebHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GalleryPhotoIdsResponse
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import kotlinx.coroutines.experimental.reactor.mono
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
	private val LAST_ID_VARIABLE = "last_id"
	private val COUNT_VARIABLE = "count"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			logger.debug("New GetGalleryPhotoIds request")

			try {
				if (!request.containsAllPathVars(LAST_ID_VARIABLE, COUNT_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotoIdsResponse.fail(ErrorCode.GalleryPhotosErrors.BadRequest))
				}

				val lastId = try {
					request.pathVariable(LAST_ID_VARIABLE).toLong()
				} catch (error: NumberFormatException) {
					Long.MAX_VALUE
				}

				val count = try {
					request.pathVariable(COUNT_VARIABLE).toInt().coerceAtMost(ServerSettings.MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT)
				} catch (error: NumberFormatException) {
					ServerSettings.MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT
				}

				val galleryPhotoIds = galleryPhotosRepository.findPaged(lastId, count)
				logger.debug("Found ${galleryPhotoIds.size} photo ids from gallery")

				return@mono formatResponse(HttpStatus.OK,
					GalleryPhotoIdsResponse.success(galleryPhotoIds))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GalleryPhotoIdsResponse.fail(ErrorCode.GalleryPhotosErrors.UnknownError))
			}
		}.flatMap { it }
	}
}