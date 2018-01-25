package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.model.ServerErrorCode
import com.kirakishou.photoexchange.model.net.response.GetUserLocationResponse
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.reactor.asMono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

class GetUserLocationHandler(
    private val jsonConverter: JsonConverterService,
    private val photoInfoRepo: PhotoInfoRepository
) : WebHandler {
    private val logger = LoggerFactory.getLogger(UploadPhotoHandler::class.java)
    private val USER_ID_QUERY_PARAM = "user_id"
    private val PHOTO_NAMES_QUERY_PARAM = "photo_names"

    override fun handle(request: ServerRequest): Mono<ServerResponse> {
        val result = async {
            logger.debug("New GetUserLocation request")

            try {
                val userIdOpt = request.queryParam(USER_ID_QUERY_PARAM)
                val photoNamesOpt = request.queryParam(PHOTO_NAMES_QUERY_PARAM)

                if (!userIdOpt.isPresent || !photoNamesOpt.isPresent) {
                    return@async formatResponse(HttpStatus.BAD_REQUEST, GetUserLocationResponse.fail(ServerErrorCode.BAD_REQUEST))
                }

                val userId = userIdOpt.get()
                val photoNames = photoNamesOpt.get()
                val photoNameList = photoNames.split(',')

                if (photoNameList.isEmpty()) {
                    return@async formatResponse(HttpStatus.BAD_REQUEST, GetUserLocationResponse.fail(ServerErrorCode.BAD_REQUEST))
                }

                val photoInfoList = photoInfoRepo.findUploadedPhotosLocations(userId, photoNameList)

                val candidateUserIdList = photoInfoList.map { it.candidateUserId }
                val candidatesCoordinates = photoInfoRepo.findPhotoByCandidateUserIdList(candidateUserIdList)

                val locationsList = candidatesCoordinates.map { candidate ->
                    val currentUserPhotoInfo = photoInfoList.first { candidate.whoUploaded == it.candidateUserId }
                    GetUserLocationResponse.UserNewLocation(currentUserPhotoInfo.photoName, candidate.lat, candidate.lon)
                }

                return@async formatResponse(HttpStatus.OK, GetUserLocationResponse.success(locationsList))
            } catch (error: Throwable) {
                logger.error("Unknown error", error)
                return@async formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, GetUserLocationResponse.fail(ServerErrorCode.UNKNOWN_ERROR))
            }
        }

        return result
                .asMono(CommonPool)
                .flatMap { it }
    }

    private fun formatResponse(httpStatus: HttpStatus, response: GetUserLocationResponse): Mono<ServerResponse> {
        val photoAnswerJson = jsonConverter.toJson(response)
        return ServerResponse.status(httpStatus).body(Mono.just(photoAnswerJson))
    }
}