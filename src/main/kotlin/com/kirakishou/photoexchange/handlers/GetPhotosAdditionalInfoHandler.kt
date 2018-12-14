package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.Utils
import core.ErrorCode
import kotlinx.coroutines.reactor.mono
import net.response.GetPhotoAdditionalInfoResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetPhotosAdditionalInfoHandler(
  jsonConverter: JsonConverterService,
  private val photoInfoRepository: PhotoInfoRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetPhotosAdditionalInfoHandler::class.java)
  private val USER_ID_VARIABLE = "user_id"
  private val PHOTO_NAMES_VARIABLE = "photo_names"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetGalleryPhotoInfo request")

      try {
        if (!request.containsAllPathVars(USER_ID_VARIABLE, PHOTO_NAMES_VARIABLE)) {
          logger.debug("Request does not contain one of the required path variables")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GetPhotoAdditionalInfoResponse.fail(ErrorCode.BadRequest))
        }

        val photoNamesString = request.pathVariable(PHOTO_NAMES_VARIABLE)
        val userId = request.pathVariable(USER_ID_VARIABLE)

        if (photoNamesString.isEmpty()) {
          logger.debug("photoNamesString is empty")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GetPhotoAdditionalInfoResponse.fail(ErrorCode.NoPhotosInRequest))
        }

        val photoNames = Utils.parsePhotoNames(
          photoNamesString,
          ServerSettings.MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT,
          ServerSettings.PHOTOS_DELIMITER
        )

        val additionalInfoResponseData = photoInfoRepository.findPhotoAdditionalInfo(userId, photoNames)
        logger.debug("Found ${additionalInfoResponseData.size} photo additional info")

        return@mono formatResponse(HttpStatus.OK, GetPhotoAdditionalInfoResponse.success(additionalInfoResponseData))
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
          GetPhotoAdditionalInfoResponse.fail(ErrorCode.UnknownError))
      }
    }.flatMap { it }
  }
}