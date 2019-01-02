package com.kirakishou.photoexchange.handlers.admin

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.DiskManipulationService
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.reactor.mono
import net.response.BanUserAndAllTheirPhotosResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class BanUserAndAllTheirPhotosHandler(
  jsonConverter: JsonConverterService,
  private val photosRepository: PhotosRepository,
  private val adminInfoRepository: AdminInfoRepository,
  private val banListRepository: BanListRepository,
  private val diskManipulationService: DiskManipulationService
) : AbstractWebHandler(jsonConverter) {
  private val logger = LoggerFactory.getLogger(BanUserAndAllTheirPhotosHandler::class.java)
  private val USER_ID_VARIABLE_PATH = "user_id"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New BanUserAndAllTheirPhotos request")

      try {
        val authToken = request.headers().header(ServerSettings.authTokenHeaderName).getOrNull(0)
        if (authToken == null) {
          logger.debug("No auth token")

          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            BanUserAndAllTheirPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        if (adminInfoRepository.adminToken != authToken) {
          logger.debug("Bad auth token: ${authToken}")

          return@mono formatResponse(
            HttpStatus.FORBIDDEN,
            BanUserAndAllTheirPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val userId = request.getStringVariable(USER_ID_VARIABLE_PATH, SharedConstants.MAX_USER_ID_LEN)
        if (userId == null) {
          logger.debug("Bad param userId ($userId)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            BanUserAndAllTheirPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val allUserPhotos = photosRepository.findAllPhotosByUserId(userId)

        if (!banAllUserIpHashes(allUserPhotos)) {
          logger.debug("Could not ban one of the ip hashes for user ${userId}")
          return@mono formatResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            BanUserAndAllTheirPhotosResponse.fail(ErrorCode.DatabaseError)
          )
        }

        replaceAllUserPhotos(allUserPhotos)

        logger.debug("User ($userId) has been banned with all their photos being replaced with placeholders")
        return@mono formatResponse(HttpStatus.OK, BanUserAndAllTheirPhotosResponse.success())
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          BanUserAndAllTheirPhotosResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }

  private suspend fun replaceAllUserPhotos(allUserPhotos: List<PhotoInfo>) {
    val photoNameList = allUserPhotos
      .distinctBy { it.photoName }
      .map { it.photoName }

    if (photoNameList.isNotEmpty()) {
      for (photoName in photoNameList) {
        try {
          diskManipulationService.replaceImagesOnDiskWithRemovedImagePlaceholder(photoName)
        } catch (error: Throwable) {
          logger.error(
            "Error while trying to replace photo with removed photo placeholder, photoName = ${photoName}",
            error
          )
        }
      }
    }
  }

  private suspend fun banAllUserIpHashes(allUserPhotos: List<PhotoInfo>): Boolean {
    val ipHashList = allUserPhotos
      .distinctBy { it.ipHash }
      .map { it.ipHash }

    if (ipHashList.isNotEmpty()) {
      if (!banListRepository.banMany(ipHashList)) {
        return false
      }
    }

    return true
  }

}