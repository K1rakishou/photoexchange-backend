package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.service.JsonConverterService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono

abstract class AbstractWebHandler(
	protected val jsonConverter: JsonConverterService
) {
	abstract fun handle(request: ServerRequest): Mono<ServerResponse>

	protected fun <T> formatResponse(httpStatus: HttpStatus, response: T): Mono<ServerResponse> {
		val photoAnswerJson = jsonConverter.toJson(response)
		return ServerResponse.status(httpStatus)
			.contentType(MediaType.APPLICATION_JSON)
			.body(Mono.just(photoAnswerJson))
	}
}