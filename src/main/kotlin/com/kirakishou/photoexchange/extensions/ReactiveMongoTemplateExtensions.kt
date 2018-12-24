package com.kirakishou.photoexchange.extensions

import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun ReactiveMongoTemplate.transactional(
  block: (ReactiveMongoOperations) -> Flux<Boolean>
): Mono<Boolean> {
  return inTransaction().execute({ txTemplate ->
    return@execute block(txTemplate)
  }).next()
}