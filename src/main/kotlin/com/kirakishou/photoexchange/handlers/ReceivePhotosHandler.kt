package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.ReceivePhotosResponse
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

class ReceivePhotosHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(ReceivePhotosHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val PHOTO_NAME_PATH_VARIABLE = "photo_names"
	private var lastTimeCheck = 0L
	private val FIVE_MINUTES = TimeUnit.MINUTES.toMillis(5)

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono {
			logger.debug("New ReceivePhotos request")

			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, PHOTO_NAME_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						ReceivePhotosResponse.fail(ErrorCode.ReceivePhotosErrors.BadRequest))
				}

				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
				val photoNames = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)
				logger.debug("UserId: $userId, photoNames: $photoNames")

				val photoNameList = photoNames.split(ServerSettings.PHOTOS_DELIMITER)
				if (photoNameList.isEmpty()) {
					logger.debug("photoNameList is empty")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						ReceivePhotosResponse.fail(ErrorCode.ReceivePhotosErrors.NoPhotosInRequest))
				}

				val photoInfoList = photoInfoRepo.findPhotosWithReceiverByPhotoNamesList(userId, photoNameList)
				val photoAnswerList = photoInfoList.map {
					ReceivePhotosResponse.ReceivedPhoto(it.second.photoId, it.first.photoName, it.second.photoName, it.second.lon, it.second.lat)
				}

				if (photoAnswerList.isEmpty()) {
					logger.debug("photoAnswerList is empty")
					return@mono formatResponse(HttpStatus.OK,
						ReceivePhotosResponse.fail(ErrorCode.ReceivePhotosErrors.NoPhotosToSendBack))
				}

				cleanUp()

				logger.debug("Found ${photoAnswerList.size} photos")
				return@mono formatResponse(HttpStatus.OK,
					ReceivePhotosResponse.success(photoAnswerList))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					ReceivePhotosResponse.fail(ErrorCode.ReceivePhotosErrors.UnknownError))
			}
		}.flatMap { it }
	}

	@Synchronized
	private suspend fun cleanUp() {
		val now = TimeUtils.getTimeFast()
		if (now - lastTimeCheck <= FIVE_MINUTES) {
			return
		}

		logger.debug("Start cleanPhotoCandidates routine")
		lastTimeCheck = now

		try {
			cleanPhotoCandidates(now - FIVE_MINUTES)
		} catch (error: Throwable) {
			logger.error("Error while cleaning up (cleanPhotoCandidates)", error)
		}

		logger.debug("End cleanPhotoCandidates routine")
	}

	private suspend fun cleanPhotoCandidates(time: Long) {
		//photoInfoRepo.cleanCandidatesFromPhotosOverTime(time)
	}
}