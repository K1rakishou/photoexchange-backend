package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.StatusResponse
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class MarkPhotoAsReceivedHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(MarkPhotoAsReceivedHandler::class.java)
	private val PHOTO_NAME_PATH_VARIABLE = "photo_name"
	private val USER_ID_PATH_VARIABLE = "user_id"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		val result = concurrentService.asyncCommon {
			logger.debug("MarkPhotoAsReceived request")

			//TODO: change photoId to photoName, client side as well
			try {
				if (!request.containsAllPathVars(PHOTO_NAME_PATH_VARIABLE, USER_ID_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						StatusResponse.from(ServerErrorCode.BAD_REQUEST))
				}

				val photoName = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)
				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)

				if (photoName.isEmpty() || userId.isEmpty()) {
					logger.debug("Either photoName or userId is empty. photoName: $photoName, userId: $userId")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						StatusResponse.from(ServerErrorCode.BAD_PHOTO_ID))
				}

				if (!photoInfoRepo.updateSetPhotoSuccessfullyDelivered(photoName, userId, TimeUtils.getTimeFast())) {
					logger.debug("Couldn't update photo delivered")
					return@asyncCommon formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						StatusResponse.from(ServerErrorCode.UNKNOWN_ERROR))
				}

				return@asyncCommon formatResponse(HttpStatus.OK, StatusResponse.from(ServerErrorCode.OK))

			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@asyncCommon formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					UploadPhotoResponse.fail(ServerErrorCode.UNKNOWN_ERROR))
			}
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}
}