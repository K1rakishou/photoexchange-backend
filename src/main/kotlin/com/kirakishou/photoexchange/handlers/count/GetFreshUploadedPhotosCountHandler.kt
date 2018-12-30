package com.kirakishou.photoexchange.handlers.count

import com.kirakishou.photoexchange.database.pgsql.repository.PhotosRepository
import com.kirakishou.photoexchange.extensions.getLongVariable
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.reactor.mono
import net.response.GetFreshPhotosCountResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetFreshUploadedPhotosCountHandler(
  jsonConverter: JsonConverterService,
  private val photosRepository: PhotosRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetFreshUploadedPhotosCountHandler::class.java)
  private val TIME_PATH_VARIABLE = "time"
  private val USER_ID_PATH_VARIABLE = "user_id"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetFreshUploadedPhotosCount request")

      try {
        val userId = request.getStringVariable(USER_ID_PATH_VARIABLE, SharedConstants.MAX_USER_ID_LEN)
        if (userId == null) {
          logger.debug("Bad param userId ($userId)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest)
          )
        }

        val time = request.getLongVariable(TIME_PATH_VARIABLE, 0L, Long.MAX_VALUE)
        if (time == null) {
          logger.debug("Bad param time ($time)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest)
          )
        }

        val freshPhotosCount = photosRepository.countFreshUploadedPhotosSince(userId, time)
        logger.debug("Found ${freshPhotosCount} fresh uploaded photos")

        return@mono formatResponse(
          HttpStatus.OK,
          GetFreshPhotosCountResponse.success(freshPhotosCount)
        )

      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          GetFreshPhotosCountResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }
}
