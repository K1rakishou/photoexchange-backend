package com.kirakishou.photoexchange.routers

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.handlers.*
import com.kirakishou.photoexchange.handlers.admin.BanPhotoHandler
import com.kirakishou.photoexchange.handlers.admin.BanUserAndAllTheirPhotosHandler
import com.kirakishou.photoexchange.handlers.admin.BanUserHandler
import com.kirakishou.photoexchange.handlers.admin.StartCleanupHandler
import com.kirakishou.photoexchange.handlers.count.GetFreshGalleryPhotosCountHandler
import com.kirakishou.photoexchange.handlers.count.GetFreshReceivedPhotosCountHandler
import com.kirakishou.photoexchange.handlers.count.GetFreshUploadedPhotosCountHandler
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.router

class Router(
  private val uploadPhotoHandler: UploadPhotoHandler,
  private val receivePhotosHandler: ReceivePhotosHandler,
  private val getPhotoHandler: GetPhotoHandler,
  private val getGalleryPhotosHandler: GetGalleryPhotosHandler,
  private val favouritePhotoHandler: FavouritePhotoHandler,
  private val reportPhotoHandler: ReportPhotoHandler,
  private val getUserUuidHandler: GetUserUuidHandler,
  private val getUploadedPhotosHandler: GetUploadedPhotosHandler,
  private val getReceivedPhotosHandler: GetReceivedPhotosHandler,
  private val getStaticMapHandler: GetStaticMapHandler,
  private val checkAccountExistsHandler: CheckAccountExistsHandler,
  private val updateFirebaseTokenHandler: UpdateFirebaseTokenHandler,
  private val getFreshGalleryPhotosCountHandler: GetFreshGalleryPhotosCountHandler,
  private val getFreshUploadedPhotosCountHandler: GetFreshUploadedPhotosCountHandler,
  private val getFreshReceivedPhotosCountHandler: GetFreshReceivedPhotosCountHandler,
  private val banPhotoHandler: BanPhotoHandler,
  private val banUserHandler: BanUserHandler,
  private val startCleanupHandler: StartCleanupHandler,
  private val getPhotosAdditionalInfoHandler: GetPhotosAdditionalInfoHandler,
  private val banUserAndAllTheirPhotosHandler: BanUserAndAllTheirPhotosHandler
) {
	private fun hasAuthHeaderPredicate() = { headers: ServerRequest.Headers ->
		headers.header(ServerSettings.authTokenHeaderName).isNotEmpty()
	}

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
					GET("/get_user_uuid", getUserUuidHandler::handle)
					GET("/receive_photos/{$USER_UUID_VARIABLE}/{$PHOTO_NAME_LIST_VARIABLE}", receivePhotosHandler::handle)
					GET("/get_photos_additional_info/{$USER_UUID_VARIABLE}/{$PHOTO_NAME_LIST_VARIABLE}", getPhotosAdditionalInfoHandler::handle)

          GET("/get_page_of_gallery_photos/{$LAST_UPLOADED_ON_VARIABLE}/{$COUNT_VARIABLE}", getGalleryPhotosHandler::handle)
					GET("/get_page_of_uploaded_photos/{$USER_UUID_VARIABLE}/{$LAST_UPLOADED_ON_VARIABLE}/{$COUNT_VARIABLE}", getUploadedPhotosHandler::handle)
					GET("/get_page_of_received_photos/{$USER_UUID_VARIABLE}/{$LAST_UPLOADED_ON_VARIABLE}/{$COUNT_VARIABLE}", getReceivedPhotosHandler::handle)

					GET("/check_account_exists/{$USER_UUID_VARIABLE}", checkAccountExistsHandler::handle)
					PUT("/favourite", favouritePhotoHandler::handle)
					PUT("/report", reportPhotoHandler::handle)

          GET("/get_fresh_uploaded_photos_count/{$USER_UUID_VARIABLE}/{$TIME_VARIABLE}", getFreshUploadedPhotosCountHandler::handle)
          GET("/get_fresh_received_photos_count/{$USER_UUID_VARIABLE}/{$TIME_VARIABLE}", getFreshReceivedPhotosCountHandler::handle)
          GET("/get_fresh_gallery_photos_count/{$TIME_VARIABLE}", getFreshGalleryPhotosCountHandler::handle)
				}

				headers(hasAuthHeaderPredicate()).nest {
					PUT("/ban_photo/{$PHOTO_NAME_VARIABLE}", banPhotoHandler::handle)
					PUT("/ban_user/{$USER_UUID_VARIABLE}", banUserHandler::handle)
					PUT("/ban_user_with_photos/{$USER_UUID_VARIABLE}", banUserAndAllTheirPhotosHandler::handle)

					//used to forcefully start cleaning up of old photos
					GET("/start_cleanup", startCleanupHandler::handle)
				}

				accept(MediaType.parseMediaType("image/*")).nest {
					GET("/get_photo/{$PHOTO_NAME_VARIABLE}/{$PHOTO_SIZE_VARIABLE}", getPhotoHandler::handle)
					GET("/get_static_map/{photo_name}", getStaticMapHandler::handle)
				}
			}
		}
	}

	companion object {
		const val USER_UUID_VARIABLE = "user_uuid"
		const val PHOTO_NAME_VARIABLE = "photo_name"
		const val TIME_VARIABLE = "time"
		const val LAST_UPLOADED_ON_VARIABLE = "last_uploaded_on"
		const val COUNT_VARIABLE = "count"
    const val PHOTO_SIZE_VARIABLE = "photo_size"
		const val PHOTO_NAME_LIST_VARIABLE = "photo_name_list"
	}
}