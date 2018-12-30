package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.mongo.repository.UserInfoRepository
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.reactor.mono
import net.response.CheckAccountExistsResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class CheckAccountExistsHandler(
  jsonConverter: JsonConverterService,
  private val userInfoRepository: UserInfoRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(CheckAccountExistsHandler::class.java)
  private val USER_ID_PATH_VARIABLE = "user_id"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New CheckAccountExists request")

      try {
        val userId = request.getStringVariable(
          USER_ID_PATH_VARIABLE,
          SharedConstants.MAX_USER_ID_LEN
        )

        if (userId == null) {
          logger.debug("Bad param userId ($userId)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            CheckAccountExistsResponse.fail(ErrorCode.BadRequest)
          )
        }

        if (!userInfoRepository.accountExists(userId)) {
          logger.debug("Account with userId ${userId} does not exist")
          return@mono formatResponse(
            HttpStatus.OK,
            CheckAccountExistsResponse.success(false)
          )
        }

        logger.debug("Account with userId ${userId} exists")
        return@mono formatResponse(
          HttpStatus.OK,
          CheckAccountExistsResponse.success(true)
        )
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          CheckAccountExistsResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }
}