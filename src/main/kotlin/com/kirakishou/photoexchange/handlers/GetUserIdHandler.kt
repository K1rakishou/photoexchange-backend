package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.GetUserIdResponse
import com.kirakishou.photoexchange.service.ConcurrencyService
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.experimental.reactor.asMono
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
		val result = concurrentService.asyncCommon {
			try {
				logger.debug("New GetUserIdHandler request")

				val userInfo = userInfoRepository.createNew()
				if (userInfo.isEmpty()) {
					return@asyncCommon formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
						GetUserIdResponse.fail(ErrorCode.GetUserIdError.DatabaseError))
				}

				return@asyncCommon formatResponse(HttpStatus.OK,
					GetUserIdResponse.success(userInfo.userId))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@asyncCommon formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					GetUserIdResponse.fail(ErrorCode.GetUserIdError.UnknownError))
			}
		}

		return result
			.asMono(concurrentService.commonThreadPool)
			.flatMap { it }
	}
}