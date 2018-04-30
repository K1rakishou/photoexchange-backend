package com.kirakishou.photoexchange.routers

import com.kirakishou.photoexchange.handlers.GetGalleryPhotosHandler
import com.kirakishou.photoexchange.handlers.GetPhotoAnswerHandler
import com.kirakishou.photoexchange.handlers.GetPhotoHandler
import com.kirakishou.photoexchange.handlers.UploadPhotoHandler
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.router

class Router(
	private val uploadPhotoHandler: UploadPhotoHandler,
	private val getPhotoAnswerHandler: GetPhotoAnswerHandler,
	private val getPhotoHandler: GetPhotoHandler,
	private val getGalleryPhotosHandler: GetGalleryPhotosHandler
) {
	fun setUpRouter() = router {
		"/v1".nest {
			"/api".nest {
				accept(MediaType.MULTIPART_FORM_DATA).nest {
					POST("/upload", uploadPhotoHandler::handle)
				}

				accept(MediaType.APPLICATION_JSON).nest {
					GET("/get_answer/{photo_names}/{user_id}", getPhotoAnswerHandler::handle)
					GET("/get_gallery_photos/{last_id}", getGalleryPhotosHandler::handle)
				}

				accept(MediaType.parseMediaType("image/*")).nest {
					GET("/get_photo/{photo_name}/{photo_size}", getPhotoHandler::handle)
				}
			}
		}
	}
}