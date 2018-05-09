package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GalleryPhotosResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.Utils
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetGalleryPhotosHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepository: PhotoInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetGalleryPhotosHandler::class.java)
	private val PHOTO_IDS_VARIABLE = "photo_ids"
	private val DELIMITER = ','

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		val result = concurrentService.asyncCommon {
			try {
				logger.debug("New GetGalleryPhotos request")

				if (!request.containsAllPathVars(PHOTO_IDS_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.BadRequest()))
				}

				val photoIdsString = request.pathVariable(PHOTO_IDS_VARIABLE)
				val galleryPhotoIds = Utils.parseGalleryPhotoIds(photoIdsString, DELIMITER)

				if (galleryPhotoIds.isEmpty()) {
					logger.debug("galleryPhotoIds is empty")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.NoPhotosInRequest()))
				}

				val resultMap = photoInfoRepository.findGalleryPhotosByIds(galleryPhotoIds)
				val galleryPhotosResponse = resultMap.values.map { (photoInfo, galleryPhoto, favouritesCount) ->
					GalleryPhotosResponse.GalleryPhotoResponseData(galleryPhoto.id, photoInfo.photoName, photoInfo.lon, photoInfo.lat,
						photoInfo.uploadedOn, favouritesCount)
				}

				logger.debug("Found ${galleryPhotosResponse.size} photos from gallery")
				return@asyncCommon formatResponse(HttpStatus.OK, GalleryPhotosResponse.success(galleryPhotosResponse))

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