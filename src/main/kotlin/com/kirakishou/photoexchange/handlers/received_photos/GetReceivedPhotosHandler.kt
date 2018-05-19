package com.kirakishou.photoexchange.handlers.received_photos

import com.kirakishou.photoexchange.handlers.AbstractWebHandler
import com.kirakishou.photoexchange.service.JsonConverterService
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

class GetReceivedPhotosHandler(
	jsonConverter: JsonConverterService
) : AbstractWebHandler(jsonConverter) {

	override fun handle(request: ServerRequest): Mono<ServerResponse> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}