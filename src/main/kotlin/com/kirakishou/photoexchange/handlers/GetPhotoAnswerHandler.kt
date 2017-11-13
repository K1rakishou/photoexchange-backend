package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.PhotoInfo
import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerJsonObject
import com.kirakishou.photoexchange.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.JsonConverterService
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GetPhotoAnswerHandler(
        private val jsonConverter: JsonConverterService,
        private val photoInfoRepo: PhotoInfoRepository
) : WebHandler {

    private val USER_ID_PATH_VARIABLE = "user_id"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val userId = request.pathVariable(USER_ID_PATH_VARIABLE)
        val countFlux = photoInfoRepo.countUserUploadedPhotos(userId)
                .map { it.toInt() }
                .flux()
                .share()

        val userHasNoUploadedPhotosFlux = countFlux
                .filter { count -> count <= 0 }
                .flatMap {
                    return@flatMap getBodyResponse(PhotoAnswerResponse.fail(ServerErrorCode.NOTHING_FOUND))
                }

        val userHasUploadedPhotosFlux = countFlux
                .filter { count -> count > 0 }
                .flatMap { count ->
                    val photoInfoResultMonoList = arrayListOf<Mono<PhotoInfo>>()

                    for (i in 0 until count) {
                        photoInfoResultMonoList += photoInfoRepo.findPhotoInfo(userId)
                    }

                    return@flatMap Flux.fromIterable(photoInfoResultMonoList)
                            .flatMap { it }
                            .buffer()
                            .single()
                }
                .zipWith(countFlux)
                .flatMap {
                    val photoInfoList = it.t1
                    val count = it.t2

                    val photoAnswerList = photoInfoList
                            .filter { !it.isEmpty() }
                            .map { photoInfo ->
                                return@map PhotoAnswerJsonObject(
                                        photoInfo.whoUploaded,
                                        photoInfo.photoName,
                                        photoInfo.lon,
                                        photoInfo.lat)
                            }

                    if (photoAnswerList.isEmpty()) {
                        return@flatMap getBodyResponse(PhotoAnswerResponse.fail(ServerErrorCode.NOTHING_FOUND))
                    }

                    val allFound = (count - photoAnswerList.size) > 0
                    return@flatMap getBodyResponse(PhotoAnswerResponse.success(photoAnswerList, allFound, ServerErrorCode.OK))
                }

        return Flux.merge(userHasNoUploadedPhotosFlux, userHasUploadedPhotosFlux)
                .single()
    }

    private fun getBodyResponse(response: PhotoAnswerResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.ok().body(Mono.just(photoAnswerJson))
    }
}