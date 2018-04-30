package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoExchangeRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.PhotoAnswer
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

class GetPhotoAnswerHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository,
	private val photoInfoExchangeRepository: PhotoInfoExchangeRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(GetPhotoAnswerHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val PHOTO_NAME_PATH_VARIABLE = "photo_names"
	private val DELIMITER = ','
	private var lastTimeCheck = 0L
	private val FIVE_MINUTES = TimeUnit.MINUTES.toMillis(5)

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		logger.debug("New GetPhotoAnswer request")

		val result = concurrentService.asyncCommon {
			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, PHOTO_NAME_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						PhotoAnswerResponse.fail(ErrorCode.GetPhotoAnswerErrors.BadRequest()))
				}

				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
				val photoNames = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)
				logger.debug("UserId: $userId, photoNames: $photoNames")

				val userUploadedPhotosCountDeferred = photoInfoRepo.countUserUploadedPhotos(userId)
				val userReceivedPhotosCountDeferred = photoInfoRepo.countUserReceivedPhotos(userId)

				val userUploadedPhotosCount = userUploadedPhotosCountDeferred.await()
				val userReceivedPhotosCount = userReceivedPhotosCountDeferred.await()
				val userCanReceivePhotosCount = userUploadedPhotosCount - userReceivedPhotosCount

				val checkUserPhotosCountResult = checkUserPhotosCount(userUploadedPhotosCount, userReceivedPhotosCount)
				if (checkUserPhotosCountResult != null) {
					return@asyncCommon checkUserPhotosCountResult
				}

				val photoNameList = photoNames.split(DELIMITER)
				if (photoNameList.isEmpty()) {
					logger.debug("photoNameList is empty")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						PhotoAnswerResponse.fail(ErrorCode.GetPhotoAnswerErrors.NoPhotosInRequest()))
				}

				if (photoNameList.size > userCanReceivePhotosCount) {
					logger.debug("photoNameList size is bigger than userCanReceivePhotosCount (${photoNameList.size}, $userCanReceivePhotosCount)")
					return@asyncCommon formatResponse(HttpStatus.BAD_REQUEST,
						PhotoAnswerResponse.fail(ErrorCode.GetPhotoAnswerErrors.TooManyPhotosRequested()))
				}

				val photoAnswerList = arrayListOf<PhotoAnswer>()

				//TODO: remake DB requests in batches
				for (uploadedPhotoName in photoNameList) {
					logger.debug("PhotoName = $uploadedPhotoName")

					val photoInfo = photoInfoRepo.find(userId, uploadedPhotoName)
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
						logger.debug("Other user has not not received photo yet (???)")
						continue
					}

					val otherUserPhotoInfo = photoInfoRepo.findByExchangeIdAndUserIdAsync(otherUserId, exchangeId).await()
					if (otherUserPhotoInfo.isEmpty()) {
						logger.debug("Could not find other user's photoInfo = otherUserId: $otherUserId, exchangeId: $exchangeId")
						continue
					}

					photoAnswerList += PhotoAnswer(uploadedPhotoName, otherUserPhotoInfo.photoName, otherUserPhotoInfo.lon, otherUserPhotoInfo.lat)
				}

				if (photoAnswerList.isEmpty()) {
					logger.debug("photoAnswerList is empty")
					return@asyncCommon formatResponse(HttpStatus.OK,
						PhotoAnswerResponse.fail(ErrorCode.GetPhotoAnswerErrors.NoPhotosToSendBack()))
				}

				cleanUp()

				return@asyncCommon formatResponse(HttpStatus.OK, PhotoAnswerResponse.success(photoAnswerList))

			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@asyncCommon formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					PhotoAnswerResponse.fail(ErrorCode.GetPhotoAnswerErrors.UnknownError()))
			}
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}

	private fun checkUserPhotosCount(userUploadedPhotosCount: Long,
									 userReceivedPhotosCount: Long): Mono<ServerResponse>? {
		logger.debug("userUploadedPhotosCount: $userUploadedPhotosCount, userReceivedPhotosCount: $userReceivedPhotosCount")

		if (userUploadedPhotosCount == -1L) {
			logger.debug("Could not get user's uploaded photos count from the DB")
			return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				PhotoAnswerResponse.fail(ErrorCode.GetPhotoAnswerErrors.DatabaseError()))
		}

		if (userReceivedPhotosCount == -1L) {
			logger.debug("Could not get user's received photos count from the DB")
			return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				PhotoAnswerResponse.fail(ErrorCode.GetPhotoAnswerErrors.DatabaseError()))
		}

		if (userUploadedPhotosCount <= userReceivedPhotosCount) {
			logger.debug("User has less (or equal) amount of uploaded photos than received")
			return formatResponse(HttpStatus.OK,
				PhotoAnswerResponse.fail(ErrorCode.GetPhotoAnswerErrors.NotEnoughPhotosUploaded()))
		}

		if (userUploadedPhotosCount == 0L) {
			logger.debug("User has not uploaded any photos yet")
			return formatResponse(HttpStatus.OK,
				PhotoAnswerResponse.fail(ErrorCode.GetPhotoAnswerErrors.NotEnoughPhotosUploaded()))
		}

		return null
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