package com.kirakishou.photoexchange.handlers.gallery_photos

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.handlers.AbstractWebHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.gallery_photos.GalleryPhotosResponse
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.util.Utils
import kotlinx.coroutines.reactor.mono
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
				if (!request.containsAllPathVars(LAST_UPLOADED_ON, COUNT)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.BadRequest))
				}

				val lastUploadedOn = try {
          request.pathVariable(LAST_UPLOADED_ON).toLong()
				} catch (error: Throwable) {
          error.printStackTrace()

          logger.debug("Bad param last_uploaded_on (${request.pathVariable(LAST_UPLOADED_ON)})")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.BadRequest))
        }

        val count = try {
          request.pathVariable(COUNT)
            .toInt()
            .coerceIn(ServerSettings.MIN_GALLERY_PHOTOS_PER_REQUEST_COUNT, ServerSettings.MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT)
        } catch (error: Throwable) {
          error.printStackTrace()

          logger.debug("Bad param count (${request.pathVariable(COUNT)})")
          return@mono formatResponse(HttpStatus.BAD_REQUEST,
            GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.BadRequest))
        }

				val galleryPhotosResponse = photoInfoRepository.findGalleryPhotos(lastUploadedOn, count)

				logger.debug("Found ${galleryPhotosResponse.size} photos from gallery")
				return@mono formatResponse(HttpStatus.OK, GalleryPhotosResponse.success(galleryPhotosResponse))

			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GalleryPhotosResponse.fail(ErrorCode.GalleryPhotosErrors.UnknownError))
			}
		}.flatMap { it }
	}
}