package com.kirakishou.photoexchange.handlers.gallery_photos

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.AbstractWebHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GalleryPhotosResponse
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.Utils
import kotlinx.coroutines.experimental.reactor.mono
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

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			logger.debug("New GetGalleryPhotos request")

			try {
				if (!request.containsAllPathVars(PHOTO_IDS_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.BadRequest))
				}

				val photoIdsString = request.pathVariable(PHOTO_IDS_VARIABLE)
				val galleryPhotoIds = Utils.parsePhotoIds(photoIdsString,
					ServerSettings.MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT,
					ServerSettings.PHOTOS_DELIMITER)

				if (galleryPhotoIds.isEmpty()) {
					logger.debug("galleryPhotoIds is empty")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.NoPhotosInRequest))
				}

				val resultMap = photoInfoRepository.findGalleryPhotosByIds(galleryPhotoIds)
				val galleryPhotosResponse = resultMap.values.map { (photoInfo, galleryPhoto, favouritesCount) ->
					GalleryPhotosResponse.GalleryPhotoResponseData(galleryPhoto.id, photoInfo.photoName, photoInfo.lon, photoInfo.lat,
						photoInfo.uploadedOn, favouritesCount)
				}

				logger.debug("Found ${galleryPhotosResponse.size} photos from gallery")
				return@mono formatResponse(HttpStatus.OK, GalleryPhotosResponse.success(galleryPhotosResponse))

			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.UnknownError))
			}
		}.flatMap { it }
	}
}