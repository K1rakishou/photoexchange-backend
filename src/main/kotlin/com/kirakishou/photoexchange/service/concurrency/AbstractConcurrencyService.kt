package com.kirakishou.photoexchange.service.concurrency

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.ThreadPoolDispatcher

abstract class AbstractConcurrencyService {
	abstract val mongoThreadPool: ThreadPoolDispatcher
	abstract val commonThreadPool: ThreadPoolDispatcher

	abstract fun <T> asyncMongo(func: suspend () -> T): Deferred<T>
	abstract fun <T> asyncCommon(func: suspend () -> T): Deferred<T>
}