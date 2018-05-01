package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GalleryPhotoAnswer
import com.kirakishou.photoexchange.model.net.response.GalleryPhotosResponse
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
			try {
				logger.debug("New GetGalleryPhotos request")

				if (!request.containsAllPathVars(LAST_ID_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.BadRequest()))
				}

				val lastId = try {
					request.pathVariable(LAST_ID_VARIABLE).toLong()
				} catch (error: NumberFormatException) {
					Long.MAX_VALUE
				}

				val photos = galleryPhotosRepository.findPaged(lastId)
				val galleryPhotos = photos.values.map { (photoInfo, galleryPhoto) ->
					GalleryPhotoAnswer(galleryPhoto.id, photoInfo.photoName, photoInfo.lon, photoInfo.lat, photoInfo.uploadedOn, galleryPhoto.likesCount)
				}

				logger.debug("Found ${galleryPhotos.size} photos from gallery")
				return@asyncCommon formatResponse(HttpStatus.OK,
					GalleryPhotosResponse.success(galleryPhotos))

			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@asyncCommon formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.UnknownError()))
			}
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}
}