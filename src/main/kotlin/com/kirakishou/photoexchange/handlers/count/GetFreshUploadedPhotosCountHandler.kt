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

class GetFreshUploadedPhotosCountHandler(
  jsonConverter: JsonConverterService,
  private val photoInfoRepo: PhotoInfoRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetFreshUploadedPhotosCountHandler::class.java)
  private val TIME_PATH_VARIABLE = "time"
  private val USER_ID_PATH_VARIABLE = "user_id"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetFreshUploadedPhotosCount request")

      try {
        if (!request.containsAllPathVars(TIME_PATH_VARIABLE, USER_ID_PATH_VARIABLE)) {
          logger.debug("Request does not contain one of the required path variables")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest))
        }

        val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
        val time = try {
          request.pathVariable(TIME_PATH_VARIABLE).toLong()
        } catch (error: NumberFormatException) {
          logger.error("Could not convert TIME_PATH_VARIABLE param to long ", error)

          logger.debug("Bad param time (${request.pathVariable(TIME_PATH_VARIABLE)})")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GetFreshPhotosCountResponse.fail(ErrorCode.BadRequest))
        }

        val freshPhotosCount = photoInfoRepo.countFreshUploadedPhotosSince(userId, time)

        logger.debug("Found ${freshPhotosCount} fresh uploaded photos")
        return@mono formatResponse(HttpStatus.OK, GetFreshPhotosCountResponse.success(freshPhotosCount))
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
          GetFreshPhotosCountResponse.fail(ErrorCode.UnknownError))
      }
    }.flatMap { it }
  }
}
