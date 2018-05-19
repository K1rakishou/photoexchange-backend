package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GetUserIdResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetUserIdHandler(
	jsonConverter: JsonConverterService,
	private val userInfoRepository: UserInfoRepository,
	private val concurrentService: ConcurrencyService
) : AbstractWebHandler(jsonConverter) {

	private val logger = LoggerFactory.getLogger(GetUserIdHandler::class.java)

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono(concurrentService.commonThreadPool) {
			try {
				logger.debug("New GetUserIdHandler request")

				val userInfo = userInfoRepository.createNew()
				if (userInfo.isEmpty()) {
					return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						GetUserIdResponse.fail(ErrorCode.GetUserIdError.DatabaseError))
				}

				return@mono formatResponse(HttpStatus.OK,
					GetUserIdResponse.success(userInfo.userId))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GetUserIdResponse.fail(ErrorCode.GetUserIdError.UnknownError))
			}
		}.flatMap { it }
	}
}