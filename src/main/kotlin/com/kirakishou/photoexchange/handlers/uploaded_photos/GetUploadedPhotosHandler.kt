package com.kirakishou.photoexchange.handlers.uploaded_photos

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.AbstractWebHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GetUploadedPhotosResponse
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import com.kirakishou.photoexchange.util.Utils
import kotlinx.coroutines.experimental.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetUploadedPhotosHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetUploadedPhotosHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val PHOTO_IDS_PATH_VARIABLE = "photo_ids"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool)  {
			logger.debug("New GetUploadedPhotos request")

			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, PHOTO_IDS_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosError.BadRequest))
				}

				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
				val photoIdsString = request.pathVariable(PHOTO_IDS_PATH_VARIABLE)

				val uploadedPhotoIds = Utils.parsePhotoIds(photoIdsString,
					ServerSettings.MAX_UPLOADED_PHOTOS_PER_REQUEST_COUNT,
					ServerSettings.PHOTOS_DELIMITER)

				if (uploadedPhotoIds.isEmpty()) {
					logger.debug("uploadedPhotoIds is empty")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosError.NoPhotosInRequest))
				}

				val uploadedPhotos = photoInfoRepo.findManyPhotos(userId, uploadedPhotoIds, true)
				val uploadedPhotosDataList = uploadedPhotos.map { uploadedPhoto ->
					GetUploadedPhotosResponse.UploadedPhoto(
						uploadedPhoto.photoInfo.photoId,
						uploadedPhoto.photoInfo.photoName,
						uploadedPhoto.lon,
						uploadedPhoto.lat)
				}

				logger.debug("Found ${uploadedPhotos.size} uploaded photos")
				return@mono formatResponse(HttpStatus.OK,
					GetUploadedPhotosResponse.success(uploadedPhotosDataList))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosError.UnknownError))
			}
		}.flatMap { it }
	}
}