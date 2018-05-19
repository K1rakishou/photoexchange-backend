package com.kirakishou.photoexchange.service.concurrency

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext

class TestConcurrencyService : AbstractConcurrencyService() {

	override val mongoThreadPool: ThreadPoolDispatcher = newFixedThreadPoolContext(1, "test1")
	override val commonThreadPool: ThreadPoolDispatcher = newFixedThreadPoolContext(1, "test2")

	override fun <T> asyncMongo(func: suspend () -> T): Deferred<T> {
		return async(mongoThreadPool) { func() }
	}

	override fun <T> asyncCommon(func: suspend () -> T): Deferred<T> {
		return async(commonThreadPool) { func() }
	}
}
