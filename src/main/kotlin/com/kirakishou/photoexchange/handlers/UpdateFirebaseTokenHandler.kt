package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.model.exception.EmptyPacket
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactor.mono
import net.request.UpdateFirebaseTokenPacket
import net.response.UpdateFirebaseTokenResponse
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpStatus
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
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
        if (!packet.isPacketOk()) {
          logger.error("One or more of the packet's fields are incorrect")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            UpdateFirebaseTokenResponse.fail(ErrorCode.BadRequest))
        }

        if (!userInfoRepository.accountExists(packet.userId)) {
          logger.error("One or more of the packet's fields are incorrect")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            //TODO: should probably add new AccountNotFound error code
            UpdateFirebaseTokenResponse.fail(ErrorCode.BadRequest))
        }

        if (!userInfoRepository.updateFirebaseToken(packet.userId, packet.token)) {
          logger.error("One or more of the packet's fields are incorrect")
          return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
            //TODO: should probably add new AccountNotFound error code
            UpdateFirebaseTokenResponse.fail(ErrorCode.DatabaseError))
        }

        return@mono formatResponse(HttpStatus.OK, UpdateFirebaseTokenResponse.success())
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
          UpdateFirebaseTokenResponse.fail(ErrorCode.UnknownError))
      }
    }.flatMap { it }
  }

  private fun collectPart(map: MultiValueMap<String, Part>, partName: String): Mono<MutableList<DataBuffer>> {
    return map.getFirst(partName)!!
      .content()
      .doOnNext { dataBuffer ->
        if (dataBuffer.readableByteCount() == 0) {
          throw EmptyPacket()
        }
      }
      .buffer()
      .single()
  }
}