package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.request.FavouritePhotoPacket
import com.kirakishou.photoexchange.model.net.response.FavouritePhotoResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.reactive.awaitSingle
import kotlinx.coroutines.experimental.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class FavouritePhotoHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepository: PhotoInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(FavouritePhotoHandler::class.java)

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			try {
				val packetBuffers = request.body(BodyExtractors.toDataBuffers())
					.buffer()
					.awaitSingle()

				val packet = jsonConverter.fromJson<FavouritePhotoPacket>(packetBuffers)
				if (!packet.isPacketOk()) {
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						FavouritePhotoResponse.fail(ErrorCode.FavouritePhotoErrors.BadRequest))
				}

				val result = photoInfoRepository.favouritePhoto(packet.userId, packet.photoName)

				return@mono when (result) {
					is PhotoInfoRepository.FavouritePhotoResult.Error -> {
						formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, FavouritePhotoResponse.fail(ErrorCode.FavouritePhotoErrors.UnknownError))
					}
					is PhotoInfoRepository.FavouritePhotoResult.Unfavourited -> {
						formatResponse(HttpStatus.OK, FavouritePhotoResponse.success(false, result.count))
					}
					is PhotoInfoRepository.FavouritePhotoResult.Favourited -> {
						formatResponse(HttpStatus.OK, FavouritePhotoResponse.success(true, result.count))
					}
				}
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					FavouritePhotoResponse.fail(ErrorCode.FavouritePhotoErrors.UnknownError))
			}
		}.flatMap { it }
	}
}