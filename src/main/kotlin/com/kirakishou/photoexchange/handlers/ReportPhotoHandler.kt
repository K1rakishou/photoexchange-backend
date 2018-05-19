package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.request.ReportPhotoPacket
import com.kirakishou.photoexchange.model.net.response.ReportPhotoResponse
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

class ReportPhotoHandler(
	jsonConverter: JsonConverterService,
	private val photoInfoRepository: PhotoInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(ReportPhotoHandler::class.java)

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			try {
				val packetBuffers = request.body(BodyExtractors.toDataBuffers())
					.buffer()
					.awaitSingle()

				val packet = jsonConverter.fromJson<ReportPhotoPacket>(packetBuffers)
				if (!packet.isPacketOk()) {
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						ReportPhotoResponse.fail(ErrorCode.ReportPhotoErrors.BadRequest))
				}

				val result = photoInfoRepository.reportPhoto(packet.userId, packet.photoName)

				return@mono when (result) {
					is PhotoInfoRepository.ReportPhotoResult.Error -> {
						formatResponse(HttpStatus.INTERNAL_SERVER_ERROR, ReportPhotoResponse.fail(ErrorCode.ReportPhotoErrors.UnknownError))
					}
					is PhotoInfoRepository.ReportPhotoResult.Unreported -> {
						formatResponse(HttpStatus.OK, ReportPhotoResponse.success(false))
					}
					is PhotoInfoRepository.ReportPhotoResult.Reported -> {
						formatResponse(HttpStatus.OK, ReportPhotoResponse.success(true))
					}
				}
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					ReportPhotoResponse.fail(ErrorCode.ReportPhotoErrors.UnknownError))
			}
		}.flatMap { it }
	}
}