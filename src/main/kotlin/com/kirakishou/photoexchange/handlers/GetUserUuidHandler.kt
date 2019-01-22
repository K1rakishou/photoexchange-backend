package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.UsersRepository
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.mono
import net.response.GetUserUuidResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetUserUuidHandler(
  private val usersRepository: UsersRepository,
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetUserUuidHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      try {
        logger.debug("New GetUserUuidHandler request")

        val userInfo = usersRepository.createNew()
        if (userInfo.isEmpty()) {
          logger.error("Could not create new user")
          return@mono formatResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            GetUserUuidResponse.fail(ErrorCode.DatabaseError)
          )
        }

        logger.debug("Successfully created new user account with userUuid (${userInfo.userUuid})")
        return@mono formatResponse(
          HttpStatus.OK,
          GetUserUuidResponse.success(userInfo.userUuid.uuid)
        )
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          GetUserUuidResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }
}