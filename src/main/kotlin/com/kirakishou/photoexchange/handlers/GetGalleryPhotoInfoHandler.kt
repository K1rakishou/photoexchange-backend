package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GalleryPhotoInfoResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.Utils
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetGalleryPhotoInfoHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepository: PhotoInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetGalleryPhotoInfoHandler::class.java)
	private val USER_ID_VARIABLE = "user_id"
	private val PHOTO_IDS_VARIABLE = "photo_ids"
	private val DELIMITER = ','

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		val result = concurrentService.asyncCommon {
			try {
				logger.debug("New GetGalleryPhotoInfo request")

				if (!request.containsAllPathVars(USER_ID_VARIABLE, PHOTO_IDS_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotoInfoResponse.fail(ErrorCode.GalleryPhotosInfoError.BadRequest()))
				}

				val photoIdsString = request.pathVariable(PHOTO_IDS_VARIABLE)
				val userId = request.pathVariable(USER_ID_VARIABLE)

				if (photoIdsString.isEmpty()) {
					logger.debug("galleryPhotoIds is empty")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotoInfoResponse.fail(ErrorCode.GalleryPhotosInfoError.NoPhotosInRequest()))
				}

				val galleryPhotoIds = Utils.parsePhotoIds(photoIdsString, ServerSettings.MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT, DELIMITER)
				val galleryPhotoInfoList = photoInfoRepository.findGalleryPhotosInfo(userId, galleryPhotoIds)

				val galleryPhotoInfoResponse = galleryPhotoInfoList.values.map { (galleryPhotoId, isFavourited, isReported) ->
					GalleryPhotoInfoResponse.GalleryPhotosInfoData(galleryPhotoId, isFavourited, isReported)
				}

				logger.debug("Found ${galleryPhotoInfoResponse.size} photo infos from gallery")
				return@asyncCommon formatResponse(HttpStatus.OK, GalleryPhotoInfoResponse.success(galleryPhotoInfoResponse))

			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@asyncCommon formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GalleryPhotoInfoResponse.fail(ErrorCode.GalleryPhotosInfoError.UnknownError()))
			}
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}
}