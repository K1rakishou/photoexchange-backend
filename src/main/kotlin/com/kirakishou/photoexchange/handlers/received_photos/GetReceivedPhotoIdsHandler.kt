package com.kirakishou.photoexchange.handlers.received_photos

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.AbstractWebHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GetReceivedPhotoIdsResponse
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import kotlinx.coroutines.experimental.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetReceivedPhotoIdsHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetReceivedPhotoIdsHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val LAST_ID_PATH_VARIABLE = "last_id"
	private val COUNT_PATH_VARIABLE = "count"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			logger.debug("New GetReceivedPhotoIds request")

			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, LAST_ID_PATH_VARIABLE, COUNT_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GetReceivedPhotoIdsResponse.fail(ErrorCode.GetReceivedPhotosError.BadRequest))
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
					countString.toInt().coerceAtMost(ServerSettings.MAX_RECEIVED_PHOTOS_PER_REQUEST_COUNT)
				} catch (error: NumberFormatException) {
					ServerSettings.MAX_RECEIVED_PHOTOS_PER_REQUEST_COUNT
				}

				val photosPage = photoInfoRepo.findReceivedPhotosPaged(userId, lastId, count)
				val receivedPhotoIds = photosPage.map { photoInfo -> photoInfo.photoId }
				logger.debug("Found ${receivedPhotoIds.size} photo ids")

				return@mono formatResponse(HttpStatus.OK,
					GetReceivedPhotoIdsResponse.success(receivedPhotoIds))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GetReceivedPhotoIdsResponse.fail(ErrorCode.GetReceivedPhotosError.UnknownError))
			}
		}.flatMap { it }
	}
}