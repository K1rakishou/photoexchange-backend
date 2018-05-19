package com.kirakishou.photoexchange.handlers.received_photos

import com.kirakishou.photoexchange.handlers.AbstractWebHandler
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetReceivedPhotoIdsHandler(
	jsonConverter: JsonConverterService,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetReceivedPhotoIdsHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"
	private val LAST_ID_PATH_VARIABLE = "last_id"
	private val COUNT_PATH_VARIABLE = "count"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		/*return mono(concurrentService.commonThreadPool) {
			logger.debug("New GetReceivedPhotoIds request")

			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE, LAST_ID_PATH_VARIABLE, COUNT_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						GetReceivedPhotoIdsResponse.fail(ErrorCode.GetReceivedPhotosError.BadRequest))
				}

				val uploaderPhotoId = request.pathVariable(USER_ID_PATH_VARIABLE)
				val lastIdString = request.pathVariable(LAST_ID_PATH_VARIABLE)
				val countString = request.pathVariable(COUNT_PATH_VARIABLE)

				logger.debug("uploaderPhotoId: $uploaderPhotoId, lastId: $lastIdString, count: $countString")

				val lastId = try {
					lastIdString.toLong()
				} catch (error: NumberFormatException) {
					Long.MAX_VALUE
				}

				val count = try {
					countString.toInt().coerceAtMost(ServerSettings.MAX_RECEIVED_PHOTOS_PER_REQUEST_COUNT)
				} catch (error: NumberFormatException) {
					ServerSettings.MAX_RECEIVED_PHOTOS_PER_REQUEST_COUNT
				}

				val photoAnswerList = arrayListOf<ReceivedPhotoResponse.ReceivedPhoto>()
				val photoInfoList = photoInfoRepo.findMany(uploaderPhotoId, photoNameList)

				//TODO: remake DB requests in batches
				for (photoInfo in photoInfoList) {
					val uploadedPhotoName = photoInfo.photoName
					logger.debug("PhotoName = $uploadedPhotoName")

					if (photoInfo.isEmpty()) {
						logger.debug("Could not find photoInfo = uploaderPhotoId: $uploaderPhotoId, uploadedPhotoName: $uploadedPhotoName")
						continue
					}

					val exchangeId = photoInfo.exchangeId
					val photoInfoExchange = photoInfoExchangeRepository.findById(exchangeId)

					val otherUserId = if (photoInfoExchange.receiverPhotoId == uploaderPhotoId) {
						photoInfoExchange.uploaderPhotoId
					} else {
						photoInfoExchange.receiverPhotoId
					}

					if (otherUserId.isEmpty()) {
						logger.debug("Other user has not not received photo yet")
						continue
					}

					val otherUserPhotoInfo = photoInfoRepo.findByExchangeIdAndUserId(otherUserId, exchangeId)
					if (otherUserPhotoInfo.isEmpty()) {
						logger.debug("Could not find other user's photoInfo = otherUserId: $otherUserId, exchangeId: $exchangeId")
						continue
					}

					photoAnswerList += ReceivedPhotoResponse.ReceivedPhoto(uploadedPhotoName, otherUserPhotoInfo.photoName,
						otherUserPhotoInfo.lon, otherUserPhotoInfo.lat)
				}

			} catch (error: Throwable) {

			}
		}.flatMap { it }*/

		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}