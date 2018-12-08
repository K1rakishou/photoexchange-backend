package com.kirakishou.photoexchange.handlers.admin

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.AdminInfoRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.AbstractWebHandler
import com.kirakishou.photoexchange.service.ImageManipulationService
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.reactor.mono
import net.response.RemovePhotoResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class BanPhotoHandler(
  jsonConverter: JsonConverterService,
  private val photoInfoRepository: PhotoInfoRepository,
  private val adminInfoRepository: AdminInfoRepository,
  private val imageManipulationService: ImageManipulationService
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(JsonConverterService::class.java)
  private val PHOTO_NAME_VARIABLE_PATH = "photo_name"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New BanPhotoHandler request")

      try {
        val authToken = request.headers().header(ServerSettings.authTokenHeaderName).getOrNull(0)
        if (authToken == null) {
          logger.debug("No auth token")

          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            RemovePhotoResponse.fail(ErrorCode.BadRequest))
        }

        if (adminInfoRepository.adminToken != authToken) {
          logger.debug("Bad auth token: ${authToken}")

          return@mono formatResponse(HttpStatus.FORBIDDEN,
            RemovePhotoResponse.fail(ErrorCode.BadRequest))
        }

        if (!request.containsAllPathVars(PHOTO_NAME_VARIABLE_PATH)) {
          logger.debug("Request does not contain one of the required path variables")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            RemovePhotoResponse.fail(ErrorCode.BadRequest))
        }

        val photoName = request.pathVariable(PHOTO_NAME_VARIABLE_PATH)

        val photoInfo = photoInfoRepository.findOneByPhotoName(photoName)
        if (photoInfo.isEmpty()) {
          logger.debug("Photo does not exist")
          return@mono formatResponse(HttpStatus.NOT_FOUND,
            RemovePhotoResponse.fail(ErrorCode.PhotoDoesNotExist))
        }

        try {
          imageManipulationService.replaceImagesOnDiskWithRemovedImagePlaceholder(photoName)
        } catch (error: Throwable) {
          logger.error("Error while trying to replace photo with removed photo placeholder", error)
          return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            RemovePhotoResponse.fail(ErrorCode.CouldNotReplacePhotoWithPlaceholder))
        }

        return@mono formatResponse(HttpStatus.OK, RemovePhotoResponse.success())
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
          RemovePhotoResponse.fail(ErrorCode.UnknownError))
      }

    }.flatMap { it }
  }

}