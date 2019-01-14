package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.core.PhotoName
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import net.request.ReportPhotoPacket
import net.response.ReportPhotoResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class ReportPhotoHandler(
  private val photosRepository: PhotosRepository,
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(ReportPhotoHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New ReportPhoto request")

      try {
        val packetBuffers = request.body(BodyExtractors.toDataBuffers())
          .buffer()
          .awaitSingle()

        val packet = jsonConverter.fromJson<ReportPhotoPacket>(packetBuffers)
        if (!isPacketOk(packet)) {
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            ReportPhotoResponse.fail(ErrorCode.BadRequest)
          )
        }

        val result = photosRepository.reportPhoto(
          UserUuid(packet.userUuid),
          PhotoName(packet.photoName)
        )

        return@mono when (result) {
          is PhotosRepository.ReportPhotoResult.Error -> {
            logger.debug("Could not report photo (${packet.photoName})")
            formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, ReportPhotoResponse.fail(ErrorCode.UnknownError))
          }
          is PhotosRepository.ReportPhotoResult.Unreported -> {
            logger.debug("A photo (${packet.photoName}) has been unreported")
            formatResponse(HttpStatus.OK, ReportPhotoResponse.success(false))
          }
          is PhotosRepository.ReportPhotoResult.Reported -> {
            logger.debug("A photo (${packet.photoName}) has been reported")
            formatResponse(HttpStatus.OK, ReportPhotoResponse.success(true))
          }
          is PhotosRepository.ReportPhotoResult.PhotoDoesNotExist -> {
            logger.debug("A photo (${packet.photoName}) does not exist")
            formatResponse(HttpStatus.NOT_FOUND, ReportPhotoResponse.fail(ErrorCode.PhotoDoesNotExist))
          }
          PhotosRepository.ReportPhotoResult.UserDoesNotExist -> {
            logger.debug("User with userUuid (${packet.userUuid}) does not exist")
            formatResponse(HttpStatus.NOT_FOUND, ReportPhotoResponse.fail(ErrorCode.AccountNotFound))
          }
        }
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          ReportPhotoResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }

  private fun isPacketOk(packet: ReportPhotoPacket): Boolean {
    if (packet.userUuid.isNullOrEmpty()) {
      logger.debug("Bad param userUuid (${packet.userUuid})")
      return false
    }

    if (packet.photoName.isNullOrEmpty()) {
      logger.debug("Bad param photoName (${packet.photoName})")
      return false
    }

    if (packet.userUuid.length > SharedConstants.FULL_USER_UUID_LEN) {
      logger.debug("Bad param userId (${packet.userUuid})")
      return false
    }

    if (packet.photoName.length > SharedConstants.MAX_PHOTO_NAME_LEN) {
      logger.debug("Bad param photoName (${packet.photoName})")
      return false
    }

    return true
  }
}