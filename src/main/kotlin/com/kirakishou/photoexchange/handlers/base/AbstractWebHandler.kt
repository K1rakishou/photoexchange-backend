package com.kirakishou.photoexchange.handlers.base

import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import kotlin.coroutines.CoroutineContext

abstract class AbstractWebHandler(
  protected val jsonConverter: JsonConverterService
) : CoroutineScope {
  private val job = Job()

  final override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Default

  abstract fun handle(request: ServerRequest): Mono<ServerResponse>

  protected fun <T> formatResponse(httpStatus: HttpStatus, response: T): Mono<ServerResponse> {
    val photoAnswerJson = jsonConverter.toJson(response)
    return ServerResponse.status(httpStatus)
      .contentType(MediaType.APPLICATION_JSON)
      .body(Mono.just(photoAnswerJson))
  }
}