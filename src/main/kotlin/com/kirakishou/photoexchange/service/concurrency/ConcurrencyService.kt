package com.kirakishou.photoexchange.service.concurrency

import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.COMMON_POOL_NAME
import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.MONGO_POOL_NAME
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext

class ConcurrencyService : AbstractConcurrencyService() {
	override val mongoThreadPool: CoroutineDispatcher
	override val commonThreadPool: CoroutineDispatcher

	init {
		val mongoThreadsCount = (Runtime.getRuntime().availableProcessors() * 2).coerceAtLeast(4)
		mongoThreadPool = newFixedThreadPoolContext(mongoThreadsCount, MONGO_POOL_NAME)

		val commonThreadsCount = (Runtime.getRuntime().availableProcessors() * 2).coerceAtLeast(4)
		commonThreadPool = newFixedThreadPoolContext(commonThreadsCount, COMMON_POOL_NAME)
	}

	override fun <T> asyncMongo(func: suspend () -> T): Deferred<T> {
		return async(mongoThreadPool) { func() }
	}

	override fun <T> asyncCommon(func: suspend () -> T): Deferred<T> {
		return async(commonThreadPool) { func() }
	}
}