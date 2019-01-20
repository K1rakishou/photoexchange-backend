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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.mono
import net.response.GetPhotosAdditionalInfoResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetPhotosAdditionalInfoHandler(
  private val photosRepository: PhotosRepository,
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetPhotosAdditionalInfoHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetGalleryPhotoInfo request")

      try {
        val photoNameListString = request.getStringVariable(
          Router.PHOTO_NAME_LIST_VARIABLE,
          ServerSettings.MAX_PHOTO_ADDITIONAL_INFO_PER_REQUEST * SharedConstants.MAX_PHOTO_NAME_LEN
        )

        if (photoNameListString == null) {
          logger.debug("Bad param photoNameListString ($photoNameListString)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetPhotosAdditionalInfoResponse.fail(ErrorCode.BadRequest)
          )
        }

        if (photoNameListString.isEmpty()) {
          logger.debug("photoNamesString is empty")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetPhotosAdditionalInfoResponse.fail(ErrorCode.NoPhotosInRequest)
          )
        }

        val userUuid = request.getStringVariable(
          Router.USER_UUID_VARIABLE,
          SharedConstants.FULL_USER_UUID_LEN
        )

        if (userUuid == null) {
          logger.debug("Bad param userUuid ($userUuid)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetPhotosAdditionalInfoResponse.fail(ErrorCode.BadRequest)
          )
        }

        val photoNameList = Utils.parsePhotoNames(
          photoNameListString,
          ServerSettings.MAX_PHOTO_ADDITIONAL_INFO_PER_REQUEST,
          ServerSettings.PHOTOS_DELIMITER
        )

        if (photoNameList == null) {
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetPhotosAdditionalInfoResponse.fail(ErrorCode.BadRequest)
          )
        }

        val additionalInfoResponseData = photosRepository.findPhotoAdditionalInfo(
          UserUuid(userUuid),
          photoNameList
        )

        logger.debug("Found ${additionalInfoResponseData.size} photo additional info")

        return@mono formatResponse(
          HttpStatus.OK,
          GetPhotosAdditionalInfoResponse.success(additionalInfoResponseData)
        )
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          GetPhotosAdditionalInfoResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }
}