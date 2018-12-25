package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.getIntVariable
import com.kirakishou.photoexchange.extensions.getLongVariable
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
  private val COUNT = "count"

  override fun handle(request: ServerRequest): Mono<ServerResponse> {
    return mono {
      logger.debug("New GetGalleryPhotos request")

      try {
        val lastUploadedOn = request.getLongVariable(LAST_UPLOADED_ON, 0L, Long.MAX_VALUE)
        if (lastUploadedOn == null) {
          logger.debug("Bad param lastUploadedOn ($lastUploadedOn)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GalleryPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val count = request.getIntVariable(
          COUNT,
          ServerSettings.MIN_GALLERY_PHOTOS_PER_REQUEST_COUNT,
          ServerSettings.MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT
        )

        if (count == null) {
          logger.debug("Bad param count ($count)")
          return@mono formatResponse(
            HttpStatus.BAD_REQUEST,
            GalleryPhotosResponse.fail(ErrorCode.BadRequest)
          )
        }

        val galleryPhotoResponseData = photoInfoRepository.findGalleryPhotos(lastUploadedOn, count)
        logger.debug("Found ${galleryPhotoResponseData.size} photos from gallery")

        val response = GalleryPhotosResponse.success(
          galleryPhotoResponseData
        )

        return@mono formatResponse(HttpStatus.OK, response)
      } catch (error: Throwable) {
        logger.error("Unknown error", error)
        return@mono formatResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          GalleryPhotosResponse.fail(ErrorCode.UnknownError)
        )
      }
    }.flatMap { it }
  }
}