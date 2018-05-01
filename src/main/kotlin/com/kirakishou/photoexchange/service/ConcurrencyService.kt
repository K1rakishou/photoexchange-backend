package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.Common.COMMON_POOL_NAME
import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.Common.COMMON_THREADS_PERCENTAGE
import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.Mongo.MONGO_POOL_NAME
import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.Mongo.MONGO_THREADS_PERCENTAGE
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.ThreadPoolDispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext

class ConcurrencyService {
	val mongoThreadPool: ThreadPoolDispatcher
	val commonThreadPool: ThreadPoolDispatcher

	init {
		val mongoThreadsCount = getThreadsCount(MONGO_THREADS_PERCENTAGE, Runtime.getRuntime().availableProcessors())
		mongoThreadPool = newFixedThreadPoolContext(mongoThreadsCount, MONGO_POOL_NAME)

		val commonThreadsCount = getThreadsCount(COMMON_THREADS_PERCENTAGE, Runtime.getRuntime().availableProcessors())
		commonThreadPool = newFixedThreadPoolContext(commonThreadsCount, COMMON_POOL_NAME)

		System.err.println("mongoThreadsCount = $mongoThreadsCount, commonThreadsCount = $commonThreadsCount")
	}

	fun getThreadsCount(percentage: Double, processorsCount: Int): Int {
		var count = (processorsCount.toDouble() * percentage).toInt()

		//have at least two threads per coroutine pool
		if (count < 2) {
			count = 2
		}

		return count
	}

	fun <T> asyncMongo(func: suspend () -> T): Deferred<T> {
		return async(mongoThreadPool) { func() }
	}

	fun <T> asyncCommon(func: suspend () -> T): Deferred<T> {
		return async(commonThreadPool) { func() }
	}
}