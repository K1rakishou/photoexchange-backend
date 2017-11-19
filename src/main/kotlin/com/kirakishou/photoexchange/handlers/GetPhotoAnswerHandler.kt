package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerJsonObject
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.reactor.asMono
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

class GetPhotoAnswerHandler(
        private val jsonConverter: JsonConverterService,
        private val photoInfoRepo: PhotoInfoRepository
) : WebHandler {

    private val USER_ID_PATH_VARIABLE = "user_id"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val result = async {
            try {
                //TODO: check USER_ID_PATH_VARIABLE existence
                val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
                val userUploadedPhotosCount = photoInfoRepo.countUserUploadedPhotos(userId)
                val userReceivedPhotosCount = photoInfoRepo.countUserReceivedBackPhotos(userId)

                if (userUploadedPhotosCount <= userReceivedPhotosCount) {
                    return@async formatResponse(HttpStatus.OK, PhotoAnswerResponse.fail(ServerErrorCode.UPLOAD_MORE_PHOTOS))
                }

                if (userUploadedPhotosCount <= 0) {
                    return@async formatResponse(HttpStatus.OK, PhotoAnswerResponse.fail(ServerErrorCode.USER_HAS_NO_UPLOADED_PHOTOS))
                }

                val photoInfo = photoInfoRepo.findPhotoInfo(userId)
                if (photoInfo.isEmpty()) {
                    return@async formatResponse(HttpStatus.OK, PhotoAnswerResponse.fail(ServerErrorCode.NO_PHOTOS_TO_SEND_BACK))
                }

                val photoAnswer = PhotoAnswerJsonObject(
                        photoInfo.photoId,
                        photoInfo.whoUploaded,
                        photoInfo.photoName,
                        photoInfo.lon,
                        photoInfo.lat)

                val allFound = (userUploadedPhotosCount - (userReceivedPhotosCount + 1)) <= 0
                return@async formatResponse(HttpStatus.OK, PhotoAnswerResponse.success(photoAnswer, allFound, ServerErrorCode.OK))

            } catch (error: Throwable) {
                error.printStackTrace()
                return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, PhotoAnswerResponse.fail(ServerErrorCode.UNKNOWN_ERROR))
            }
        }

        return result
                .asMono(CommonPool)
                .flatMap { it }
    }

    private fun formatResponse(httpStatus: HttpStatus, response: PhotoAnswerResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.status(httpStatus).body(Mono.just(photoAnswerJson))
    }
}