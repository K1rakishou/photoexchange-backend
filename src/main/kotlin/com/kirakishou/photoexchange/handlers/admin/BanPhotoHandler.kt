package com.kirakishou.photoexchange.handlers.admin

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.AdminInfoRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.DiskManipulationService
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.reactor.mono
import net.response.BanPhotoResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class BanPhotoHandler(
  jsonConverter: JsonConverterService,
  private val photoInfoRepository: PhotoInfoRepository,
  private val adminInfoRepository: AdminInfoRepository,
  private val diskManipulationService: DiskManipulationService
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(BanPhotoHandler::class.java)
  private val PHOTO_NAME_VARIABLE_PATH = "photo_name"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New BanPhoto request")

      try {
        val authToken = request.headers().header(ServerSettings.authTokenHeaderName).getOrNull(0)
        if (authToken == null) {
          logger.debug("No auth token")

          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            BanPhotoResponse.fail(ErrorCode.BadRequest)
          )
        }

        if (adminInfoRepository.adminToken != authToken) {
          logger.debug("Bad auth token: ${authToken}")

          return@mono formatResponse(
            HttpStatus.FORBIDDEN,
            BanPhotoResponse.fail(ErrorCode.BadRequest)
          )
        }

        val photoName = request.getStringVariable(PHOTO_NAME_VARIABLE_PATH, SharedConstants.MAX_PHOTO_NAME_LEN)
        if (photoName == null) {
          logger.debug("Bad param photoName ($photoName)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            BanPhotoResponse.fail(ErrorCode.BadRequest)
          )
        }

        val photoInfo = photoInfoRepository.findOneByPhotoName(photoName)
        if (photoInfo.isEmpty()) {
          logger.debug("Photo does not exist")
          return@mono formatResponse(
            HttpStatus.NOT_FOUND,
            BanPhotoResponse.fail(ErrorCode.PhotoDoesNotExist)
          )
        }

        try {
          diskManipulationService.replaceImagesOnDiskWithRemovedImagePlaceholder(photoName)
        } catch (error: Throwable) {
          logger.error(
            "Error while trying to replace photo with removed photo placeholder, photoName = ${photoName}",
            error
          )
          return@mono formatResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            BanPhotoResponse.fail(ErrorCode.CouldNotReplacePhotoWithPlaceholder)
          )
        }

        logger.debug("Photo (${photoName}) banned")
        return@mono formatResponse(HttpStatus.OK, BanPhotoResponse.success())
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          BanPhotoResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }

}