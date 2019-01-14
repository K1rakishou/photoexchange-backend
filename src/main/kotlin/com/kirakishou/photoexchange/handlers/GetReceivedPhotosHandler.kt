package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.extensions.getIntVariable
import com.kirakishou.photoexchange.extensions.getLongVariable
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.mono
import net.response.ReceivedPhotosResponse
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetReceivedPhotosHandler(
  private val photosRepository: PhotosRepository,
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetReceivedPhotosHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetReceivedPhotos request")

      try {
        val lastUploadedOn = request.getLongVariable(
          Router.LAST_UPLOADED_ON_VARIABLE,
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
          Router.COUNT_VARIABLE,
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

        val userUuid = request.getStringVariable(
          Router.USER_UUID_VARIABLE,
          SharedConstants.FULL_USER_UUID_LEN
        )

        if (userUuid.isNullOrEmpty()) {
          logger.debug("Bad param userUuid ($userUuid)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            ReceivedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val receivedPhotosResponseData = photosRepository.findPageOfReceivedPhotos(
          UserUuid(userUuid),
          DateTime(lastUploadedOn),
          count
        )

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