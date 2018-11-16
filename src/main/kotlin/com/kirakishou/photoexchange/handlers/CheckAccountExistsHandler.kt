package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.extensions.containsAllPathVars
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.CheckAccountExistsResponse
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.reactor.mono
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class CheckAccountExistsHandler(
	jsonConverter: JsonConverterService,
	private val userInfoRepository: UserInfoRepository
) : AbstractWebHandler(jsonConverter) {
	private val logger = LoggerFactory.getLogger(CheckAccountExistsHandler::class.java)
	private val USER_ID_PATH_VARIABLE = "user_id"

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		return mono {
			logger.debug("New CheckAccountExists request")

			try {
				if (!request.containsAllPathVars(USER_ID_PATH_VARIABLE)) {
					logger.debug("Request does not contain one of the required path variables")
					return@mono formatResponse(HttpStatus.BAD_REQUEST,
						CheckAccountExistsResponse.fail(ErrorCode.CheckAccountExistsErrors.BadRequest))
				}

				val userId = request.pathVariable(USER_ID_PATH_VARIABLE)

				if (!userInfoRepository.accountExists(userId)) {
					logger.debug("Account with userId ${userId} does not exist")
					return@mono formatResponse(HttpStatus.OK,
						CheckAccountExistsResponse.success(false))
				}

				logger.debug("Account with userId ${userId} exists")
				return@mono formatResponse(HttpStatus.OK,
					CheckAccountExistsResponse.success(true))
			} catch (error: Throwable) {
				logger.error("Unknown error", error)
				return@mono formatResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					CheckAccountExistsResponse.fail(ErrorCode.CheckAccountExistsErrors.UnknownError))
			}
		}.flatMap { it }
	}
}