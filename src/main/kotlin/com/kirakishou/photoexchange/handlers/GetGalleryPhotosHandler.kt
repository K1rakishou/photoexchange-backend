package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
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
	private val photoInfoRepository: PhotoInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetGalleryPhotosHandler::class.java)
	private val USER_ID_VARIABLE = "user_id"
	private val PHOTO_IDS_VARIABLE = "photo_ids"
	private val DELIMITER = ','

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		val result = concurrentService.asyncCommon {
			logger.debug("New GetGalleryPhotos request")

			if (!request.containsAllPathVars(PHOTO_IDS_VARIABLE, USER_ID_VARIABLE)) {
				logger.debug("Request does not contain one of the required path variables")
				return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
					GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.BadRequest()))
			}

			val userId = request.pathVariable(USER_ID_VARIABLE)
			val photoIdsString = request.pathVariable(PHOTO_IDS_VARIABLE)
			val galleryPhotoIds = parseGalleryPhotoIds(photoIdsString)

			if (galleryPhotoIds.isEmpty()) {
				logger.debug("galleryPhotoIds is empty")
				return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
					GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.NoPhotosInRequest()))
			}

			val resultMap = photoInfoRepository.findPhotoInfoListByGalleryPhotoIdList(userId, galleryPhotoIds)
			val galleryPhotosResponse = resultMap.values.map { (photoInfo, galleryPhoto, isFavourited, isReported) ->
				GalleryPhotosResponse.GalleryPhotoResponseData(galleryPhoto.id, photoInfo.photoName, photoInfo.lon, photoInfo.lat,
					photoInfo.uploadedOn, photoInfo.favouritesCount, isFavourited, isReported)
			}

			return@asyncCommon formatResponse(HttpStatus.OK, GalleryPhotosResponse.success(galleryPhotosResponse))
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}

	private fun parseGalleryPhotoIds(photoIdsString: String): List<Long> {
		return photoIdsString
			.split(DELIMITER)
			.map { photoId ->
				return@map try {
					photoId.toLong()
				} catch (error: NumberFormatException) {
					-1L
				}
			}
			.filter { it != -1L }
	}
}