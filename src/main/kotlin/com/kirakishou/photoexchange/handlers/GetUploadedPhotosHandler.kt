package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.getIntVariable
import com.kirakishou.photoexchange.extensions.getLongVariable
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
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

        val lastUploadedOn = request.getLongVariable(
          LAST_UPLOADED_ON,
          0L,
          Long.MAX_VALUE
        )

        if (lastUploadedOn == null) {
          logger.debug("Bad param lastUploadedOn ($lastUploadedOn)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetUploadedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val count = request.getIntVariable(
          COUNT,
          ServerSettings.MIN_UPLOADED_PHOTOS_PER_REQUEST_COUNT,
          ServerSettings.MAX_UPLOADED_PHOTOS_PER_REQUEST_COUNT
        )

        if (count == null) {
          logger.debug("Bad param count ($count)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetUploadedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val userId = request.getStringVariable(
          USER_ID,
          SharedConstants.MAX_USER_ID_LEN
        )

        if (userId == null) {
          logger.debug("Bad param userId ($userId)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetUploadedPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val uploadedPhotos = photoInfoRepository.findPageOfUploadedPhotos(userId, lastUploadedOn, count)
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