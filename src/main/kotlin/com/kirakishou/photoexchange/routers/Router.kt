package com.kirakishou.photoexchange.routers

import com.kirakishou.photoexchange.handlers.*
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.router

class Router(
        private val uploadPhotoHandler: UploadPhotoHandler,
        private val getPhotoAnswerHandler: GetPhotoAnswerHandler,
        private val getPhotoHandler: GetPhotoHandler,
        private val markPhotoAsReceived: MarkPhotoAsReceivedHandler,
        private val getUserLocationHandler: GetUserLocationHandler
) {

    fun setUpRouter() = router {
        "/v1".nest {
            "/api".nest {
                accept(MediaType.MULTIPART_FORM_DATA).nest {
                    POST("/upload", uploadPhotoHandler::handle)
                }

                accept(MediaType.APPLICATION_JSON).nest {
                    POST("/received/{photo_id}/{user_id}", markPhotoAsReceived::handle)
                    GET("/get_answer/{user_id}", getPhotoAnswerHandler::handle)
                }

                GET("/get_location", getUserLocationHandler::handle)

                accept(MediaType.parseMediaType("image/*")).nest {
                    GET("/get_photo/{photo_name}/{photo_size}", getPhotoHandler::handle)
                }
            }
        }
    }
}