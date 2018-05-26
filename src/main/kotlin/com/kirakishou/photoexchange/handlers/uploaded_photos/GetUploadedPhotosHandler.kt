package com.kirakishou.photoexchange.handlers.uploaded_photos

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoExchangeRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.AbstractWebHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.uploaded_photos.GetUploadedPhotosResponse
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import com.kirakishou.photoexchange.util.Utils
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetUploadedPhotosHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepository: PhotoInfoRepository,
	private val photoInfoExchangeRepository: PhotoInfoExchangeRepository,
	private val concurrentService: AbstractConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetUploadedPhotosHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val PHOTO_IDS_PATH_VARIABLE = "photo_ids"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			logger.debug("New GetUploadedPhotos request")

			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, PHOTO_IDS_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosErrors.BadRequest))
				}

				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
				val photoIdsString = request.pathVariable(PHOTO_IDS_PATH_VARIABLE)

				val uploadedPhotoIds = Utils.parsePhotoIds(photoIdsString,
					ServerSettings.MAX_UPLOADED_PHOTOS_PER_REQUEST_COUNT,
					ServerSettings.PHOTOS_DELIMITER)

				if (uploadedPhotoIds.isEmpty()) {
					logger.debug("uploadedPhotoIds is empty")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosErrors.NoPhotosInRequest))
				}

				val uploadedPhotos = photoInfoRepository.findManyPhotos(userId, uploadedPhotoIds, true)
				val exchangeIdList = uploadedPhotos.map { it.photoInfo.exchangeId }
				val photoExchangeMap = photoInfoExchangeRepository.findMany(exchangeIdList).awaitFirst()
					.associateBy { it.id }

				val uploadedPhotosDataList = getUploadedPhotos(uploadedPhotos, photoExchangeMap)

				logger.debug("Found ${uploadedPhotos.size} uploaded photos")
				return@mono formatResponse(HttpStatus.OK,
					GetUploadedPhotosResponse.success(uploadedPhotosDataList))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosErrors.UnknownError))
			}
		}.flatMap { it }
	}

	private fun getUploadedPhotos(
		uploadedPhotos: List<PhotoInfoRepository.PhotoInfoWithLocation>,
		photoExchangeMap: Map<Long, PhotoInfoExchange>
	): List<GetUploadedPhotosResponse.UploadedPhoto> {
		return uploadedPhotos.map { uploadedPhoto ->
			val photoInfoExchange = photoExchangeMap.getOrElse(uploadedPhoto.photoInfo.exchangeId, { PhotoInfoExchange.empty() })
			val hasReceiverInfo = photoInfoExchange
				.takeIf { !it.isEmpty() }?.receiverUserId?.isNotEmpty() ?: false

			GetUploadedPhotosResponse.UploadedPhoto(
				uploadedPhoto.photoInfo.photoId,
				uploadedPhoto.photoInfo.photoName,
				uploadedPhoto.lon,
				uploadedPhoto.lat,
				hasReceiverInfo,
				uploadedPhoto.photoInfo.uploadedOn)
		}
	}
}