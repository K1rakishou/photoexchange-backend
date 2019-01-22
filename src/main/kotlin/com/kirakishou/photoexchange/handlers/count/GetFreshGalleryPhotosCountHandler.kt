package com.kirakishou.photoexchange.handlers.count

import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.extensions.getDateTimeVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.mono
import net.response.GetFreshPhotosCountResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetFreshGalleryPhotosCountHandler(
  private val photosRepository: PhotosRepository,
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetFreshGalleryPhotosCountHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetFreshGalleryPhotosCount request")

      try {
        val time = request.getDateTimeVariable(Router.TIME_VARIABLE)
        if (time == null) {
          logger.error("Bad param time ($time)")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest))
        }

        val freshPhotosCount = photosRepository.countFreshGalleryPhotosSince(time)
        logger.debug("Found ${freshPhotosCount} fresh gallery photos")

        return@mono formatResponse(HttpStatus.OK, GetFreshPhotosCountResponse.success(freshPhotosCount))
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, GetFreshPhotosCountResponse.fail(ErrorCode.UnknownError))
      }
    }.flatMap { it }
  }
}