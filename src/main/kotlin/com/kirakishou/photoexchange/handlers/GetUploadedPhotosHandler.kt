package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GetUploadedPhotosResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.reactor.asMono
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
	private val MAX_PHOTOS_PER_REQUEST_COUNT = 50
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val LAST_ID_PATH_VARIABLE = "last_id"
	private val COUNT_PATH_VARIABLE = "count"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		val result = concurrentService.asyncCommon {
			logger.debug("New UploadPhoto request")

			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, LAST_ID_PATH_VARIABLE, COUNT_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosError.BadRequest()))
				}

				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
				val lastIdString = request.pathVariable(LAST_ID_PATH_VARIABLE)
				val countString = request.pathVariable(COUNT_PATH_VARIABLE)

				logger.debug("userId: $userId, lastId: $lastIdString, count: $countString")

				val lastId = try {
					lastIdString.toLong()
				} catch (error: NumberFormatException) {
					Long.MAX_VALUE
				}

				val count = try {
					countString.toInt().coerceAtMost(MAX_PHOTOS_PER_REQUEST_COUNT)
				} catch (error: NumberFormatException) {
					MAX_PHOTOS_PER_REQUEST_COUNT
				}

				val photosPage = photoInfoRepo.findPaged(userId, lastId, count)
				val uploadedPhotos = photosPage.map { photoInfo ->
					GetUploadedPhotosResponse.UploadedPhoto(photoInfo.photoId, photoInfo.photoName)
				}

				return@asyncCommon formatResponse(HttpStatus.OK, GetUploadedPhotosResponse.success(uploadedPhotos))

			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@asyncCommon formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GetUploadedPhotosResponse.fail(ErrorCode.GetUploadedPhotosError.UnknownError()))
			}
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}
}