package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.extensions.getDateTimeVariable
import com.kirakishou.photoexchange.extensions.getIntVariable
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.mono
import net.response.GetUploadedPhotosResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetUploadedPhotosHandler(
  private val photosRepository: PhotosRepository,
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetUploadedPhotosHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetUploadedPhotos request")

      try {

        val lastUploadedOn = request.getDateTimeVariable(
          Router.LAST_UPLOADED_ON_VARIABLE
        )

        if (lastUploadedOn == null) {
          logger.error("Bad param lastUploadedOn ($lastUploadedOn)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetUploadedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val count = request.getIntVariable(
          Router.COUNT_VARIABLE,
          ServerSettings.MIN_UPLOADED_PHOTOS_PER_REQUEST_COUNT,
          ServerSettings.MAX_UPLOADED_PHOTOS_PER_REQUEST_COUNT
        )

        if (count == null) {
          logger.error("Bad param count ($count)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetUploadedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val userUuid = request.getStringVariable(
          Router.USER_UUID_VARIABLE,
          SharedConstants.FULL_USER_UUID_LEN
        )

        if (userUuid == null) {
          logger.error("Bad param userUuid ($userUuid)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetUploadedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val uploadedPhotos = photosRepository.findPageOfUploadedPhotos(
          UserUuid(userUuid),
          lastUploadedOn,
          count
        )

        logger.debug("Found ${uploadedPhotos.size} uploaded photos")

        return@mono formatResponse(
          HttpStatus.OK,
          GetUploadedPhotosResponse.success(uploadedPhotos)
        )
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          GetUploadedPhotosResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }
}