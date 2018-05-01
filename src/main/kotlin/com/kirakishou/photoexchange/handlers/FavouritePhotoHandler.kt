package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.request.FavouritePhotoPacket
import com.kirakishou.photoexchange.model.net.response.FavouritePhotoResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.reactive.awaitSingle
import kotlinx.coroutines.experimental.reactor.asMono
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class FavouritePhotoHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepository: PhotoInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		val result = concurrentService.asyncCommon {
			val packet = request.bodyToMono(FavouritePhotoPacket::class.java).awaitSingle()
			val result = photoInfoRepository.favouritePhoto(packet.userId, packet.photoName)

			return@asyncCommon when (result) {
				is PhotoInfoRepository.FavouritePhotoResult.Error -> {
					formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, FavouritePhotoResponse.fail(ErrorCode.FavouritePhotoErrors.UnknownError()))
				}
				is PhotoInfoRepository.FavouritePhotoResult.AlreadyFavourited -> {
					formatResponse(HttpStatus.OK, FavouritePhotoResponse.fail(ErrorCode.FavouritePhotoErrors.AlreadyFavourited()))
				}
				is PhotoInfoRepository.FavouritePhotoResult.Ok -> {
					formatResponse(HttpStatus.OK, FavouritePhotoResponse.success())
				}
			}
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}
}