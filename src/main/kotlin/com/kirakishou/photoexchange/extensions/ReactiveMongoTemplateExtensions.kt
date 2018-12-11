package com.kirakishou.photoexchange.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import kotlinx.coroutines.reactor.mono

suspend fun <T> ReactiveMongoTemplate.transactional(
  coroutineScope: CoroutineScope,
  block: suspend () -> T
): T {

  return inTransaction().execute {
    return@execute coroutineScope.mono {
      return@mono block()
    }
  }
    .single()
    .awaitFirst()
}