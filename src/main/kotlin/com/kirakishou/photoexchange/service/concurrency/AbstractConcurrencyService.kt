package com.kirakishou.photoexchange.service.concurrency

import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Deferred

abstract class AbstractConcurrencyService {
	abstract val mongoThreadPool: CoroutineDispatcher
	abstract val commonThreadPool: CoroutineDispatcher
	abstract val googleMapThreadPool: CoroutineDispatcher

	abstract fun <T> asyncMongo(func: suspend () -> T): Deferred<T>
	abstract fun <T> asyncCommon(func: suspend () -> T): Deferred<T>
	abstract fun <T> asyncMap(func: suspend () -> T): Deferred<T>
}