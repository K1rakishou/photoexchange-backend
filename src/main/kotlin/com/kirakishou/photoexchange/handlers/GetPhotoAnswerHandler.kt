package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerJsonObject
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

class GetPhotoAnswerHandler(
	private val jsonConverter: JsonConverterService,
	private val photoInfoRepo: PhotoInfoRepository
) : WebHandler {
	private val logger = LoggerFactory.getLogger(GetPhotoAnswerHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val PHOTO_NAME_PATH_VARIABLE = "photo_name"
	private var lastTimeCheck = 0L
	private val FIVE_MINUTES = TimeUnit.MINUTES.toMillis(5)

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		val result = async {
			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, PHOTO_NAME_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@async formatResponse(HttpStatus.BAD_REQUEST,
						PhotoAnswerResponse.fail(ServerErrorCode.BAD_REQUEST))
				}

				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
				val photoName = request.pathVariable(PHOTO_NAME_PATH_VARIABLE)
				logger.debug("New GetPhotoAnswer request. UserId: $userId, photoName: $photoName")

				val userUploadedPhotosCount = photoInfoRepo.countUserUploadedPhotos(userId)
				val userReceivedPhotosCount = photoInfoRepo.countUserReceivedPhotos(userId)

				val checkUserPhotosCountResult = checkUserPhotosCount(userUploadedPhotosCount, userReceivedPhotosCount)
				if (checkUserPhotosCountResult != null) {
					return@async checkUserPhotosCountResult
				}

				val photoInfo = photoInfoRepo.find(userId, photoName)
				if (photoInfo.isEmpty()) {
					logger.debug("No spare photos were found.")
					return@async formatResponse(HttpStatus.OK, PhotoAnswerResponse.fail(ServerErrorCode.NO_PHOTOS_TO_SEND_BACK))
				}

				val photoAnswer = PhotoAnswerJsonObject(
					photoInfo.photoId,
					photoInfo.whoUploaded,
					photoInfo.photoName,
					photoInfo.lon,
					photoInfo.lat)

				try {
					cleanUp()
				} catch (error: Throwable) {
					logger.error("Error while cleaning up (cleanPhotoCandidates)", error)
				}

				//"userReceivedPhotosCount + 1" because we are receiving a photo right now
				//and it's not marked in the database at the point of executing "photoInfoRepo.countUserReceivedBackPhotos" method
				val allFound = (userUploadedPhotosCount - (userReceivedPhotosCount + 1)) <= 0

				logger.debug("Spare photo has been found. User received the same amount of photos as he has uploaded: $allFound")
				return@async formatResponse(HttpStatus.OK, PhotoAnswerResponse.success(photoAnswer, allFound))

			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, PhotoAnswerResponse.fail(ServerErrorCode.UNKNOWN_ERROR))
			}
		}

		return result
			.asMono(CommonPool)
			.flatMap { it }
	}

	private fun checkUserPhotosCount(userUploadedPhotosCount: Long, userReceivedPhotosCount: Long): Mono<ServerResponse>? {
		if (userUploadedPhotosCount == -1L) {
			logger.debug("Could not get user's uploaded photos count from the DB")
			return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				PhotoAnswerResponse.fail(ServerErrorCode.REPOSITORY_ERROR))
		}

		if (userReceivedPhotosCount == -1L) {
			logger.debug("Could not get user's received photos count from the DB")
			return formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				PhotoAnswerResponse.fail(ServerErrorCode.REPOSITORY_ERROR))
		}

		if (userUploadedPhotosCount <= userReceivedPhotosCount) {
			logger.debug("User has less (or equal) amount of uploaded photos than received")
			return formatResponse(HttpStatus.OK,
				PhotoAnswerResponse.fail(ServerErrorCode.UPLOAD_MORE_PHOTOS))
		}

		if (userUploadedPhotosCount <= 0) {
			logger.debug("User has not uploaded any photos yet")
			return formatResponse(HttpStatus.OK,
				PhotoAnswerResponse.fail(ServerErrorCode.UPLOAD_MORE_PHOTOS))
		}

		return null
	}

	@Synchronized
	private suspend fun cleanUp() {
		val now = TimeUtils.getTimeFast()
		if (now - lastTimeCheck > FIVE_MINUTES) {
			logger.debug("Start cleanPhotoCandidates routine")

			lastTimeCheck = now
			cleanPhotoCandidates(now - FIVE_MINUTES)

			logger.debug("End cleanPhotoCandidates routine")
		}
	}

	private suspend fun cleanPhotoCandidates(time: Long) {
		//photoInfoRepo.cleanCandidatesFromPhotosOverTime(time)
	}

	private fun formatResponse(httpStatus: HttpStatus, response: PhotoAnswerResponse): Mono<ServerResponse> {
		val photoAnswerJson = jsonConverter.toJson(response)
		return ServerResponse.status(httpStatus).body(Mono.just(photoAnswerJson))
	}
}