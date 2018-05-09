package com.kirakishou.photoexchange.routers

import com.kirakishou.photoexchange.handlers.*
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.router

class Router(
	private val uploadPhotoHandler: UploadPhotoHandler,
	private val getPhotoAnswerHandler: GetPhotoAnswerHandler,
	private val getPhotoHandler: GetPhotoHandler,
	private val getGalleryPhotoIdsHandler: GetGalleryPhotoIdsHandler,
	private val getGalleryPhotosHandler: GetGalleryPhotosHandler,
	private val favouritePhotoHandler: FavouritePhotoHandler,
	private val reportPhotoHandler: ReportPhotoHandler,
	private val getUserIdHandler: GetUserIdHandler,
	private val getGalleryPhotoInfoHandler: GetGalleryPhotoInfoHandler
) {
	fun setUpRouter() = router {
		"/v1".nest {
			"/api".nest {
				accept(MediaType.MULTIPART_FORM_DATA).nest {
					POST("/upload", uploadPhotoHandler::handle)
				}

				accept(MediaType.APPLICATION_JSON).nest {
					GET("/get_answer/{photo_names}/{user_id}", getPhotoAnswerHandler::handle)
					GET("/get_gallery_photo_ids/{last_id}/{count}", getGalleryPhotoIdsHandler::handle)
					GET("/get_gallery_photos/{photo_ids}", getGalleryPhotosHandler::handle)
					GET("/get_gallery_photo_info/{user_id}/{photo_ids}", getGalleryPhotoInfoHandler::handle)
					GET("/get_user_id", getUserIdHandler::handle)

					PUT("/favourite", favouritePhotoHandler::handle)
					PUT("/report", reportPhotoHandler::handle)
				}

				accept(MediaType.parseMediaType("image/*")).nest {
					GET("/get_photo/{photo_name}/{photo_size}", getPhotoHandler::handle)
				}
			}
		}
	}
}