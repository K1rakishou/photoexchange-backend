package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.reactor.mono
import net.response.ReceivedPhotosResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class ReceivePhotosHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepository: PhotoInfoRepository
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(ReceivePhotosHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val PHOTO_NAME_PATH_VARIABLE = "photo_names"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono {
			logger.debug("New ReceivePhotos request")

			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, PHOTO_NAME_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						ReceivedPhotosResponse.fail(ErrorCode.BadRequest))
				}

				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
				val photoNames = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)
				logger.debug("UserId: $userId, photoNames: $photoNames")

				val photoNameList = photoNames.split(ServerSettings.PHOTOS_DELIMITER)
				if (photoNameList.isEmpty()) {
					logger.debug("photoNameList is empty")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						ReceivedPhotosResponse.fail(ErrorCode.NoPhotosInRequest))
				}

				val receivedPhotosResponseData = photoInfoRepository.findPhotosWithReceiverByPhotoNamesList(
          userId,
          photoNameList
        )
        
				if (receivedPhotosResponseData.isEmpty()) {
					logger.debug("photoAnswerList is empty")
					return@mono formatResponse(HttpStatus.OK,
						ReceivedPhotosResponse.fail(ErrorCode.NoPhotosToSendBack))
				}

        logger.debug("Found ${receivedPhotosResponseData.size} photos")

				val response = ReceivedPhotosResponse.success(
					receivedPhotosResponseData
				)

				return@mono formatResponse(HttpStatus.OK, response)
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					ReceivedPhotosResponse.fail(ErrorCode.UnknownError))
			}
		}.flatMap { it }
	}

}