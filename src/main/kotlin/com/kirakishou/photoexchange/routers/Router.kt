package com.kirakishou.photoexchange.routers

import com.kirakishou.photoexchange.handlers.*
import com.kirakishou.photoexchange.handlers.gallery_photos.GetGalleryPhotoInfoHandler
import com.kirakishou.photoexchange.handlers.gallery_photos.GetGalleryPhotosHandler
import com.kirakishou.photoexchange.handlers.GetReceivedPhotosHandler
import com.kirakishou.photoexchange.handlers.GetUploadedPhotosHandler
import com.kirakishou.photoexchange.handlers.count.GetFreshGalleryPhotosCountHandler
import com.kirakishou.photoexchange.handlers.count.GetFreshReceivedPhotosCountHandler
import com.kirakishou.photoexchange.handlers.count.GetFreshUploadedPhotosCountHandler
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.router

class Router(
	private val uploadPhotoHandler: UploadPhotoHandler,
	private val receivePhotosHandler: ReceivePhotosHandler,
	private val getPhotoHandler: GetPhotoHandler,
	private val getGalleryPhotosHandler: GetGalleryPhotosHandler,
	private val favouritePhotoHandler: FavouritePhotoHandler,
	private val reportPhotoHandler: ReportPhotoHandler,
	private val getUserIdHandler: GetUserIdHandler,
	private val getGalleryPhotoInfoHandler: GetGalleryPhotoInfoHandler,
	private val getUploadedPhotosHandler: GetUploadedPhotosHandler,
	private val getReceivedPhotosHandler: GetReceivedPhotosHandler,
	private val getStaticMapHandler: GetStaticMapHandler,
	private val checkAccountExistsHandler: CheckAccountExistsHandler,
  private val updateFirebaseTokenHandler: UpdateFirebaseTokenHandler,
	private val getFreshGalleryPhotosCountHandler: GetFreshGalleryPhotosCountHandler,
  private val getFreshUploadedPhotosCountHandler: GetFreshUploadedPhotosCountHandler,
  private val getFreshReceivedPhotosCountHandler: GetFreshReceivedPhotosCountHandler
) {
	fun setUpRouter() = router {
		"/v1".nest {
			"/api".nest {
				accept(MediaType.MULTIPART_FORM_DATA).nest {
					POST("/upload", uploadPhotoHandler::handle)
				}
        accept(MediaType.APPLICATION_JSON).nest {
          POST("/update_token", updateFirebaseTokenHandler::handle)
        }

				accept(MediaType.APPLICATION_JSON).nest {
					GET("/get_user_id", getUserIdHandler::handle)
					GET("/receive_photos/{photo_names}/{user_id}", receivePhotosHandler::handle)

					GET("/get_gallery_photo_info/{user_id}/{photo_names}", getGalleryPhotoInfoHandler::handle)
          GET("/get_page_of_gallery_photos/{last_uploaded_on}/{count}", getGalleryPhotosHandler::handle)
					GET("/get_page_of_uploaded_photos/{user_id}/{last_uploaded_on}/{count}", getUploadedPhotosHandler::handle)
					GET("/get_page_of_received_photos/{user_id}/{last_uploaded_on}/{count}", getReceivedPhotosHandler::handle)

					GET("/check_account_exists/{user_id}", checkAccountExistsHandler::handle)
					PUT("/favourite", favouritePhotoHandler::handle)
					PUT("/report", reportPhotoHandler::handle)

          GET("/get_fresh_uploaded_photos_count/{user_id}/{time}", getFreshUploadedPhotosCountHandler::handle)
          GET("/get_fresh_received_photos_count/{user_id}/{time}", getFreshReceivedPhotosCountHandler::handle)
          GET("/get_fresh_gallery_photos_count/{time}", getFreshGalleryPhotosCountHandler::handle)
				}

				accept(MediaType.parseMediaType("image/*")).nest {
					GET("/get_photo/{photo_name}/{photo_size}", getPhotoHandler::handle)
					GET("/get_static_map/{photo_name}", getStaticMapHandler::handle)
				}
			}
		}
	}
}