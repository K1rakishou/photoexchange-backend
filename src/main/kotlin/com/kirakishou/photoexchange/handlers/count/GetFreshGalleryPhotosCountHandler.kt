package com.kirakishou.photoexchange.handlers.count

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.reactor.mono
import net.response.GetFreshPhotosCountResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import java.lang.NumberFormatException

class GetFreshGalleryPhotosCountHandler(
  jsonConverter: JsonConverterService,
  private val photoInfoRepo: PhotoInfoRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetFreshGalleryPhotosCountHandler::class.java)
  private val TIME_PATH_VARIABLE = "time"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetFreshGalleryPhotosCount request")

      try {
        if (!request.containsAllPathVars(TIME_PATH_VARIABLE)) {
          logger.debug("Request does not contain one of the required path variables")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest))
        }

        val time = try {
          request.pathVariable(TIME_PATH_VARIABLE).toLong()
        } catch (error: NumberFormatException) {
          logger.error("Could not convert TIME_PATH_VARIABLE param to long ", error)

          logger.debug("Bad param time (${request.pathVariable(TIME_PATH_VARIABLE)})")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest))
        }

        val freshPhotosCount = photoInfoRepo.countFreshGalleryPhotosSince(time)

        logger.debug("Found ${freshPhotosCount} fresh gallery photos")
        return@mono formatResponse(HttpStatus.OK,
          GetFreshPhotosCountResponse.success(freshPhotosCount))
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
          GetFreshPhotosCountResponse.fail(ErrorCode.UnknownError))
      }
    }.flatMap { it }
  }
}