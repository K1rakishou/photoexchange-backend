package com.kirakishou.photoexchange.handlers.admin

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.Photo
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.repository.AdminInfoRepository
import com.kirakishou.photoexchange.database.repository.BanListRepository
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.extensions.getStringVariable
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.DiskManipulationService
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import core.SharedConstants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.mono
import net.response.BanUserAndAllTheirPhotosResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class BanUserAndAllTheirPhotosHandler(
  private val photosRepository: PhotosRepository,
  private val adminInfoRepository: AdminInfoRepository,
  private val banListRepository: BanListRepository,
  private val diskManipulationService: DiskManipulationService,
  dispatcher: CoroutineDispatcher,
  jsonConverter: JsonConverterService
) : AbstractWebHandler(dispatcher, jsonConverter) {
  private val logger = LoggerFactory.getLogger(BanUserAndAllTheirPhotosHandler::class.java)

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New BanUserAndAllTheirPhotos request")

      try {
        val authToken = request.headers().header(ServerSettings.authTokenHeaderName).getOrNull(0)
        if (authToken == null) {
          logger.error("No auth token")

          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            BanUserAndAllTheirPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        if (adminInfoRepository.adminToken != authToken) {
          logger.error("Bad auth token: ${authToken}")

          return@mono formatResponse(
            HttpStatus.FORBIDDEN,
            BanUserAndAllTheirPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val userUuid = request.getStringVariable(
          Router.USER_UUID_VARIABLE,
          SharedConstants.FULL_USER_UUID_LEN
        )

        if (userUuid == null) {
          logger.error("Bad param userUuid ($userUuid)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            BanUserAndAllTheirPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val allUserPhotos = photosRepository.findAllPhotosByUserUuid(UserUuid(userUuid))
        if (!banAllUserIpHashes(allUserPhotos)) {
          logger.error("Could not ban one of the ip hashes for userUuid ${userUuid}")
          return@mono formatResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            BanUserAndAllTheirPhotosResponse.fail(ErrorCode.DatabaseError)
          )
        }

        replaceAllUserPhotos(allUserPhotos)

        logger.debug("User with userUuid ($userUuid) has been banned with all their photos being replaced with placeholders")
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

  private suspend fun replaceAllUserPhotos(allUserPhotos: List<Photo>) {
    val photoNameList = allUserPhotos
      .distinctBy { it.photoName }
      .map { it.photoName }

    if (photoNameList.isNotEmpty()) {
      for (photoName in photoNameList) {
        try {
          diskManipulationService.replaceImagesOnDiskWithRemovedImagePlaceholder(photoName)
        } catch (error: Throwable) {
          logger.error(
            "Error while trying to replace photo with removed photo placeholder, photoName = ${photoName.name}",
            error
          )
        }
      }
    }
  }

  private suspend fun banAllUserIpHashes(allUserPhotos: List<Photo>): Boolean {
    val banInfoList = allUserPhotos
      .distinctBy { it.ipHash }
      .map { Pair(it.userId, it.ipHash) }

    if (banInfoList.isNotEmpty()) {
      val userIdList = banInfoList.map { it.first }
      val ipHashList = banInfoList.map { it.second }

      if (!banListRepository.banMany(userIdList, ipHashList)) {
        return false
      }
    }

    return true
  }

}