package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoExchangeRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.ReceivedPhotoResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.experimental.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

class ReceivePhotosHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository,
	private val photoInfoExchangeRepository: PhotoInfoExchangeRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(ReceivePhotosHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val PHOTO_NAME_PATH_VARIABLE = "photo_names"
	private val DELIMITER = ','
	private var lastTimeCheck = 0L
	private val FIVE_MINUTES = TimeUnit.MINUTES.toMillis(5)

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			logger.debug("New ReceivePhotosH request")

			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, PHOTO_NAME_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						ReceivedPhotoResponse.fail(ErrorCode.ReceivePhotosErrors.BadRequest))
				}

				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
				val photoNames = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)
				logger.debug("UserId: $userId, photoNames: $photoNames")

				val photoNameList = photoNames.split(DELIMITER)
				if (photoNameList.isEmpty()) {
					logger.debug("photoNameList is empty")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						ReceivedPhotoResponse.fail(ErrorCode.ReceivePhotosErrors.NoPhotosInRequest))
				}

				val photoAnswerList = arrayListOf<ReceivedPhotoResponse.ReceivedPhoto>()
				val photoInfoList = photoInfoRepo.findMany(userId, photoNameList)

				//TODO: remake DB requests in batches
				for (photoInfo in photoInfoList) {
					val uploadedPhotoName = photoInfo.photoName
					logger.debug("PhotoName = $uploadedPhotoName")

					if (photoInfo.isEmpty()) {
						logger.debug("Could not find photoInfo = userId: $userId, uploadedPhotoName: $uploadedPhotoName")
						continue
					}

					val exchangeId = photoInfo.exchangeId
					val photoInfoExchange = photoInfoExchangeRepository.findById(exchangeId)

					val otherUserId = if (photoInfoExchange.receiverUserId == userId) {
						photoInfoExchange.uploaderUserId
					} else {
						photoInfoExchange.receiverUserId
					}

					if (otherUserId.isEmpty()) {
						logger.debug("Other user has not not received photo yet")
						continue
					}

					val otherUserPhotoInfo = photoInfoRepo.findByExchangeIdAndUserId(otherUserId, exchangeId)
					if (otherUserPhotoInfo.isEmpty()) {
						logger.debug("Could not find other user's photoInfo = otherUserId: $otherUserId, exchangeId: $exchangeId")
						continue
					}

					photoAnswerList += ReceivedPhotoResponse.ReceivedPhoto(uploadedPhotoName, otherUserPhotoInfo.photoName,
						otherUserPhotoInfo.lon, otherUserPhotoInfo.lat)
				}

				if (photoAnswerList.isEmpty()) {
					logger.debug("photoAnswerList is empty")
					return@mono formatResponse(HttpStatus.OK,
						ReceivedPhotoResponse.fail(ErrorCode.ReceivePhotosErrors.NoPhotosToSendBack))
				}

				cleanUp()

				return@mono formatResponse(HttpStatus.OK, ReceivedPhotoResponse.success(photoAnswerList))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					ReceivedPhotoResponse.fail(ErrorCode.ReceivePhotosErrors.UnknownError))
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