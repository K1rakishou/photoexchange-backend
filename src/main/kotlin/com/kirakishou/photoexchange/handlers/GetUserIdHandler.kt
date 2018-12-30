package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.mongo.repository.UserInfoRepository
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.reactor.mono
import net.response.GetUserIdResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetUserIdHandler(
  jsonConverter: JsonConverterService,
  private val userInfoRepository: UserInfoRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(GetUserIdHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      try {
        logger.debug("New GetUserIdHandler request")

        val userInfo = userInfoRepository.createNew()
        if (userInfo.isEmpty()) {
          return@mono formatResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            GetUserIdResponse.fail(ErrorCode.DatabaseError)
          )
        }

        logger.debug("Successfully created new uploaderPhotoId = ${userInfo.userId}")
        return@mono formatResponse(
          HttpStatus.OK,
          GetUserIdResponse.success(userInfo.userId)
        )
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          GetUserIdResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }
}