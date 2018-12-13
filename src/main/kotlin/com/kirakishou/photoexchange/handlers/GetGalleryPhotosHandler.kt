package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.base.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import kotlinx.coroutines.reactor.mono
import net.response.GalleryPhotosResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetGalleryPhotosHandler(
  jsonConverter: JsonConverterService,
  private val photoInfoRepository: PhotoInfoRepository
) : AbstractWebHandler(jsonConverter) {

  private val logger = LoggerFactory.getLogger(GetGalleryPhotosHandler::class.java)
  private val LAST_UPLOADED_ON = "last_uploaded_on"
  private val USER_ID = "user_id"
  private val COUNT = "count"
  private val EMPTY_USER_ID = "empty_user_id"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetGalleryPhotos request")

      try {
        if (!request.containsAllPathVars(LAST_UPLOADED_ON, USER_ID, COUNT)) {
          logger.debug("Request does not contain one of the required path variables")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GalleryPhotosResponse.fail(ErrorCode.BadRequest))
        }

        val lastUploadedOn = try {
          request.pathVariable(LAST_UPLOADED_ON).toLong()
        } catch (error: Throwable) {
          error.printStackTrace()

          logger.debug("Bad param last_uploaded_on (${request.pathVariable(LAST_UPLOADED_ON)})")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GalleryPhotosResponse.fail(ErrorCode.BadRequest))
        }

        val count = try {
          request.pathVariable(COUNT)
            .toInt()
            .coerceIn(ServerSettings.MIN_GALLERY_PHOTOS_PER_REQUEST_COUNT, ServerSettings.MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT)
        } catch (error: Throwable) {
          error.printStackTrace()

          logger.debug("Bad param count (${request.pathVariable(COUNT)})")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GalleryPhotosResponse.fail(ErrorCode.BadRequest))
        }

        val userId = try {
          request.pathVariable(USER_ID)
        } catch (error: Throwable) {
          error.printStackTrace()

          EMPTY_USER_ID
        }

        val galleryPhotoResponseData = photoInfoRepository.findGalleryPhotos(lastUploadedOn, count)
        logger.debug("Found ${galleryPhotoResponseData.size} photos from gallery")

        val additionalInfoResponseData = if (userId != EMPTY_USER_ID) {
          val galleryPhotoNameList = galleryPhotoResponseData.map { it.photoName }
          photoInfoRepository.findPhotoAdditionalInfo(userId, galleryPhotoNameList)
        } else {
          emptyList()
        }

        val response = GalleryPhotosResponse.success(
          galleryPhotoResponseData,
          additionalInfoResponseData
        )

        return@mono formatResponse(HttpStatus.OK, response)
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
          GalleryPhotosResponse.fail(ErrorCode.UnknownError))
      }
    }.flatMap { it }
  }
}