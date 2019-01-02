package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.repository.UsersRepository
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.routers.Router
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
  private val usersRepository: UsersRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(CheckAccountExistsHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New CheckAccountExists request")

      try {
        val userUuid = request.getStringVariable(
          Router.USER_UUID_VARIABLE,
          SharedConstants.MAX_USER_UUID_LEN
        )

        if (userUuid == null) {
          logger.debug("Bad param userUuid ($userUuid)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            CheckAccountExistsResponse.fail(ErrorCode.BadRequest)
          )
        }

        if (!usersRepository.accountExists(UserUuid(userUuid))) {
          logger.debug("Account with userUuid ${userUuid} does not exist")
          return@mono formatResponse(
            HttpStatus.OK,
            CheckAccountExistsResponse.success(false)
          )
        }

        logger.debug("Account with userUuid ${userUuid} exists")
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