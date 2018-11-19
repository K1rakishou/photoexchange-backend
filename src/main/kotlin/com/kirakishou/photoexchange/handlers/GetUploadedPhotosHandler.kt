package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.reactor.mono
import net.response.GetUploadedPhotosResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetUploadedPhotosHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepository: PhotoInfoRepository
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetUploadedPhotosHandler::class.java)
	private val USER_ID = "user_id"
	private val LAST_UPLOADED_ON = "last_uploaded_on"
	private val COUNT = "count"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono {
			logger.debug("New GetUploadedPhotos request")

			try {
				if (!request.containsAllPathVars(USER_ID, LAST_UPLOADED_ON, COUNT)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GetUploadedPhotosResponse.fail(ErrorCode.BadRequest))
				}

				val lastUploadedOn = try {
					request.pathVariable(LAST_UPLOADED_ON).toLong()
				} catch (error: Throwable) {
					error.printStackTrace()

					logger.debug("Bad param last_uploaded_on (${request.pathVariable(LAST_UPLOADED_ON)})")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GetUploadedPhotosResponse.fail(ErrorCode.BadRequest))
				}

				val count = try {
					request.pathVariable(COUNT)
						.toInt()
						.coerceIn(ServerSettings.MIN_UPLOADED_PHOTOS_PER_REQUEST_COUNT, ServerSettings.MAX_UPLOADED_PHOTOS_PER_REQUEST_COUNT)
				} catch (error: Throwable) {
					error.printStackTrace()

					logger.debug("Bad param count (${request.pathVariable(COUNT)})")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GetUploadedPhotosResponse.fail(ErrorCode.BadRequest))
				}

        val userId = request.pathVariable(USER_ID)
        if (userId.isNullOrEmpty()) {
          logger.debug("Bad param userId (${request.pathVariable(USER_ID)})")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GetUploadedPhotosResponse.fail(ErrorCode.BadRequest))
        }

				val uploadedPhotos = photoInfoRepository.findPageOfUploadedPhotos(userId, lastUploadedOn, count)
				logger.debug("Found ${uploadedPhotos.size} uploaded photos")

				return@mono formatResponse(HttpStatus.OK,
					GetUploadedPhotosResponse.success(uploadedPhotos))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GetUploadedPhotosResponse.fail(ErrorCode.UnknownError))
			}
		}.flatMap { it }
	}
}