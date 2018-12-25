package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import net.request.UpdateFirebaseTokenPacket
import net.response.UpdateFirebaseTokenResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class UpdateFirebaseTokenHandler(
  private val userInfoRepository: UserInfoRepository,
  jsonConverterService: JsonConverterService
) : AbstractWebHandler(jsonConverterService) {

  private val logger = LoggerFactory.getLogger(UpdateFirebaseTokenHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New UpdateFirebaseToken request")

      try {
        val packetParts = request.body(BodyExtractors.toDataBuffers())
          .buffer()
          .single()
          .awaitFirst()

        val packet = jsonConverter.fromJson<UpdateFirebaseTokenPacket>(packetParts)
        if (!isPacketOk(packet)) {
          logger.error("One or more of the packet's fields is incorrect")

          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            UpdateFirebaseTokenResponse.fail(ErrorCode.BadRequest))
        }

        if (!userInfoRepository.accountExists(packet.userId)) {
          logger.error("One or more of the packet's fields are incorrect")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            UpdateFirebaseTokenResponse.fail(ErrorCode.AccountNotFound))
        }

        if (!userInfoRepository.updateFirebaseToken(packet.userId, packet.token)) {
          logger.error("One or more of the packet's fields are incorrect")
          return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            UpdateFirebaseTokenResponse.fail(ErrorCode.AccountNotFound))
        }

        return@mono formatResponse(HttpStatus.OK, UpdateFirebaseTokenResponse.success())
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
          UpdateFirebaseTokenResponse.fail(ErrorCode.UnknownError))
      }
    }.flatMap { it }
  }

  private fun isPacketOk(packet: UpdateFirebaseTokenPacket): Boolean {
    if (packet.userId.isNullOrEmpty()) {
      logger.debug("Bad param userId (${packet.userId})")
      return false
    }

    if (packet.token.isNullOrEmpty()) {
      logger.debug("Bad param token (${packet.token})")
      return false
    }

    if (packet.userId.length > SharedConstants.MAX_USER_ID_LEN) {
      logger.debug("Bad param userId (${packet.userId})")
      return false
    }

    if (packet.token.length > SharedConstants.MAX_FIREBASE_TOKEN_LEN) {
      logger.debug("Bad param token (${packet.token})")
      return false
    }

    return true
  }
}