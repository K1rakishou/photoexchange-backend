package com.kirakishou.photoexchange.handlers.count

import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.extensions.getLongVariable
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.mono
import net.response.GetFreshPhotosCountResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetFreshUploadedPhotosCountHandler(
  private val photosRepository: PhotosRepository,
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetFreshUploadedPhotosCountHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetFreshUploadedPhotosCount request")

      try {
        val userUuid = request.getStringVariable(Router.USER_UUID_VARIABLE, SharedConstants.MAX_USER_UUID_LEN)
        if (userUuid == null) {
          logger.debug("Bad param userUuid ($userUuid)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest)
          )
        }

        val time = request.getLongVariable(Router.TIME_VARIABLE, 0L, Long.MAX_VALUE)
        if (time == null) {
          logger.debug("Bad param time ($time)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest)
          )
        }

        val freshPhotosCount = photosRepository.countFreshUploadedPhotosSince(UserUuid(userUuid), time)
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
