package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.pgsql.repository.PhotosRepository
import com.kirakishou.photoexchange.extensions.getIntVariable
import com.kirakishou.photoexchange.extensions.getLongVariable
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

class GetReceivedPhotosHandler(
  jsonConverter: JsonConverterService,
  private val photosRepository: PhotosRepository
) : AbstractWebHandler(jsonConverter) {

  private val logger = LoggerFactory.getLogger(GetReceivedPhotosHandler::class.java)
  private val USER_ID = "user_id"
  private val LAST_UPLOADED_ON = "last_uploaded_on"
  private val COUNT = "count"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetReceivedPhotos request")

      try {
        val lastUploadedOn = request.getLongVariable(
          LAST_UPLOADED_ON,
          0L,
          Long.MAX_VALUE
        )

        if (lastUploadedOn == null) {
          logger.debug("Bad param lastUploadedOn ($lastUploadedOn)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            ReceivedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val count = request.getIntVariable(
          COUNT,
          ServerSettings.MIN_RECEIVED_PHOTOS_PER_REQUEST_COUNT,
          ServerSettings.MAX_RECEIVED_PHOTOS_PER_REQUEST_COUNT
        )

        if (count == null) {
          logger.debug("Bad param count ($count)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            ReceivedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val userId = request.pathVariable(USER_ID)
        if (userId.isNullOrEmpty()) {
          logger.debug("Bad param userId (${request.pathVariable(USER_ID)})")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            ReceivedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val receivedPhotosResponseData = photosRepository.findPageOfReceivedPhotos(userId, lastUploadedOn, count)
        logger.debug("Found ${receivedPhotosResponseData.size} received photos")

        val response = ReceivedPhotosResponse.success(
          receivedPhotosResponseData
        )

        return@mono formatResponse(HttpStatus.OK, response)
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          ReceivedPhotosResponse.fail(ErrorCode.UnknownError)
        )
      }

    }.flatMap { it }
  }
}