package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.Utils
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.reactor.mono
import net.response.ReceivedPhotosResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class ReceivePhotosHandler(
  jsonConverter: JsonConverterService,
  private val photosRepository: PhotosRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(ReceivePhotosHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New ReceivePhotos request")

      try {
        val userUuid = request.getStringVariable(
          Router.USER_UUID_VARIABLE,
          SharedConstants.MAX_USER_UUID_LEN
        )

        if (userUuid == null) {
          logger.debug("Bad param userUuid ($userUuid)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            ReceivedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val photoNames = request.getStringVariable(
          Router.PHOTO_NAME_LIST_VARIABLE,
          ServerSettings.MAX_RECEIVED_PHOTOS_PER_REQUEST_COUNT * SharedConstants.MAX_PHOTO_NAME_LEN
        )

        if (photoNames == null) {
          logger.debug("Bad param photoNames ($photoNames)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            ReceivedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val photoNameList = Utils.parsePhotoNames(
          photoNames,
          ServerSettings.MAX_RECEIVED_PHOTOS_PER_REQUEST_COUNT,
          ServerSettings.PHOTOS_DELIMITER
        )

        if (photoNameList.isEmpty()) {
          logger.debug("photoNameList is empty")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            ReceivedPhotosResponse.fail(ErrorCode.NoPhotosInRequest)
          )
        }

        val receivedPhotosResponseData = photosRepository.findPhotosWithReceiverByPhotoNamesList(
          UserUuid(userUuid),
          photoNameList
        )

        if (receivedPhotosResponseData.isEmpty()) {
          logger.debug("photoAnswerList is empty")
          return@mono formatResponse(
            HttpStatus.OK,
            ReceivedPhotosResponse.fail(ErrorCode.NoPhotosToSendBack)
          )
        }

        logger.debug("Found ${receivedPhotosResponseData.size} photos")

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