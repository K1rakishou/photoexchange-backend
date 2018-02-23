package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.Common.COMMON_POOL_NAME
import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.Common.COMMON_THREADS_PERCENTAGE
import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.Mongo.MONGO_POOL_NAME
import com.kirakishou.photoexchange.config.ServerSettings.ThreadPool.Mongo.MONGO_THREADS_PERCENTAGE
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.newFixedThreadPoolContext

class ConcurrencyService {
	val mongoThreadPool = newFixedThreadPoolContext(getThreadsCount(MONGO_THREADS_PERCENTAGE,
		Runtime.getRuntime().availableProcessors()), MONGO_POOL_NAME)
	val commonThreadPool = newFixedThreadPoolContext(getThreadsCount(COMMON_THREADS_PERCENTAGE,
		Runtime.getRuntime().availableProcessors()), COMMON_POOL_NAME)

	fun getThreadsCount(percentage: Double, processorsCount: Int): Int {
		var count = (processorsCount.toDouble() * percentage).toInt()
		if (count < 1) {
			count = 1
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