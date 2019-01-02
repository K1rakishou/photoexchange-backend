package com.kirakishou.photoexchange.handlers.admin

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.CleanupService
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.reactor.mono
import net.response.StartCleanupResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class StartCleanupHandler(
  jsonConverter: JsonConverterService,
  private val adminInfoRepository: AdminInfoRepository,
  private val cleanupService: CleanupService
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(StartCleanupHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New StartCleanup request")

      try {
        val authToken = request.headers().header(ServerSettings.authTokenHeaderName).getOrNull(0)
        if (authToken == null) {
          logger.debug("No auth token")

          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            StartCleanupResponse.fail(ErrorCode.BadRequest))
        }

        if (adminInfoRepository.adminToken != authToken) {
          logger.debug("Bad auth token: ${authToken}")

          return@mono formatResponse(HttpStatus.FORBIDDEN,
            StartCleanupResponse.fail(ErrorCode.BadRequest))
        }

        cleanupService.startCleaningRoutine()

        return@mono formatResponse(HttpStatus.OK, StartCleanupResponse.success())
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
          StartCleanupResponse.fail(ErrorCode.UnknownError))
      }

    }.flatMap { it }
  }
}