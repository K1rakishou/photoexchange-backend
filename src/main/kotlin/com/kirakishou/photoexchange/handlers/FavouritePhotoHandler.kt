package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import net.request.FavouritePhotoPacket
import net.response.FavouritePhotoResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class FavouritePhotoHandler(
  jsonConverter: JsonConverterService,
  private val photoInfoRepository: PhotoInfoRepository
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(FavouritePhotoHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New FavouritePhoto request")

      try {
        val packetBuffers = request.body(BodyExtractors.toDataBuffers())
          .buffer()
          .awaitSingle()

        val packet = jsonConverter.fromJson<FavouritePhotoPacket>(packetBuffers)
        if (!isPacketOk(packet)) {
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            FavouritePhotoResponse.fail(ErrorCode.BadRequest)
          )
        }

        val result = photoInfoRepository.favouritePhoto(
          packet.userId,
          packet.photoName
        )

        return@mono when (result) {
          is PhotoInfoRepository.FavouritePhotoResult.Error -> {
            logger.debug("Could not favoutite photo (${packet.photoName})")
            formatResponse(
              HttpStatus.INTERNAL_SERVER_ERROR,
              FavouritePhotoResponse.fail(ErrorCode.UnknownError)
            )
          }
          is PhotoInfoRepository.FavouritePhotoResult.Unfavourited -> {
            logger.debug("A photo (${packet.photoName}) has been unfavourited")
            formatResponse(
              HttpStatus.OK,
              FavouritePhotoResponse.success(false, result.count)
            )
          }
          is PhotoInfoRepository.FavouritePhotoResult.Favourited -> {
            logger.debug("A photo (${packet.photoName}) has been favourited")
            formatResponse(
              HttpStatus.OK,
              FavouritePhotoResponse.success(true, result.count)
            )
          }
          is PhotoInfoRepository.FavouritePhotoResult.PhotoDoesNotExist -> {
            logger.debug("A photo (${packet.photoName}) does not exist")
            formatResponse(
              HttpStatus.NOT_FOUND,
              FavouritePhotoResponse.fail(ErrorCode.PhotoDoesNotExist)
            )
          }
        }
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          FavouritePhotoResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }

  private fun isPacketOk(packet: FavouritePhotoPacket): Boolean {
    if (packet.userId.isNullOrEmpty()) {
      logger.debug("Bad param userId (${packet.userId})")
      return false
    }

    if (packet.photoName.isNullOrEmpty()) {
      logger.debug("Bad param photoName (${packet.photoName})")
      return false
    }

    if (packet.userId.length > SharedConstants.MAX_USER_ID_LEN) {
      logger.debug("Bad param userId (${packet.userId})")
      return false
    }

    if (packet.photoName.length > SharedConstants.MAX_PHOTO_NAME_LEN) {
      logger.debug("Bad param photoName (${packet.photoName})")
      return false
    }

    return true
  }
}