package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.exception.NoPhotosToSendBack
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.JsonConverterService
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
        /*//TODO: check USER_ID_PATH_VARIABLE existence
        val userId = request.pathVariable(USER_ID_PATH_VARIABLE)

        val userPhotosCountFlux = photoInfoRepo.countUserUploadedPhotos(userId)
                .map { it.toInt() }
                .flux()
                .doOnError {
                    println(it)
                }
                .share()

        val hasUserReceivedAllPhotosBack = photoInfoRepo.countUserReceivedBackPhotos(userId)
                .map { it.toInt() }
                .doOnError {
                    println(it)
                }
                .flux()
                .zipWith(userPhotosCountFlux)
                .map {
                    val photosUploaded = it.t1
                    val photosReceived = it.t2
                    val allFound = (photosUploaded - photosReceived) > 0

                    println("allFound: $allFound")
                    return@map allFound
                }
                .single()

        val userHasNoUploadedPhotosFlux = userPhotosCountFlux
                .filter { count -> count <= 0 }
                .flatMap { formatResponse(HttpStatus.OK, PhotoAnswerResponse.fail(ServerErrorCode.USER_HAS_NO_UPLOADED_PHOTOS)) }

        val userHasUploadedPhotosFlux = userPhotosCountFlux
                .filter { count -> count > 0 }
                .flatMap { photoInfoRepo.findPhotoInfo(userId) }
                .map { photoInfo ->
                    if (photoInfo.isEmpty()) {
                        throw NoPhotosToSendBack()
                    }

                    return@map PhotoAnswerJsonObject(
                            photoInfo.photoId,
                            photoInfo.whoUploaded,
                            photoInfo.photoName,
                            photoInfo.lon,
                            photoInfo.lat)
                }
                .zipWith(hasUserReceivedAllPhotosBack)
                .flatMap {
                    val photoAnswer = it.t1
                    val allFound = it.t2

                    return@flatMap formatResponse(HttpStatus.OK, PhotoAnswerResponse.success(photoAnswer, allFound, ServerErrorCode.OK))
                }

        return Flux.merge(userHasNoUploadedPhotosFlux, userHasUploadedPhotosFlux)
                .doOnError {
                    println(it)
                }
                .single()
                .onErrorResume(this::handleErrors)*/

        TODO()
    }

    private fun handleErrors(error: Throwable) = when (error) {
        is NoPhotosToSendBack ->
            formatResponse(HttpStatus.OK, PhotoAnswerResponse.fail(ServerErrorCode.NO_PHOTOS_TO_SEND_BACK))

        else -> {
            error.printStackTrace()
            formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, PhotoAnswerResponse.fail(ServerErrorCode.UNKNOWN_ERROR))
        }
    }

    private fun formatResponse(httpStatus: HttpStatus, response: PhotoAnswerResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.status(httpStatus).body(Mono.just(photoAnswerJson))
    }
}