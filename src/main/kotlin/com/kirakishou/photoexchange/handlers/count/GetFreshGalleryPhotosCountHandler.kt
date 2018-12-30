package com.kirakishou.photoexchange.handlers.count

import com.kirakishou.photoexchange.database.pgsql.repository.PhotosRepository
import com.kirakishou.photoexchange.extensions.getLongVariable
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

class GetFreshGalleryPhotosCountHandler(
  jsonConverter: JsonConverterService,
  private val photosRepository: PhotosRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetFreshGalleryPhotosCountHandler::class.java)
  private val TIME_PATH_VARIABLE = "time"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetFreshGalleryPhotosCount request")

      try {
        val time = request.getLongVariable(TIME_PATH_VARIABLE, 0L, Long.MAX_VALUE)
        if (time == null) {
          logger.debug("Bad param time ($time)")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest))
        }

        val freshPhotosCount = photosRepository.countFreshGalleryPhotosSince(time)

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