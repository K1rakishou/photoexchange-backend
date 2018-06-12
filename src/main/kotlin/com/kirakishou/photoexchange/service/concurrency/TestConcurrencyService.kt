package com.kirakishou.photoexchange.service.concurrency

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.async

class TestConcurrencyService : AbstractConcurrencyService() {
	override val mongoThreadPool = Unconfined
	override val commonThreadPool = Unconfined
	override val googleMapThreadPool = Unconfined

	override fun <T> asyncMongo(func: suspend () -> T): Deferred<T> {
		return async(CommonPool) { func() }
	}

	override fun <T> asyncCommon(func: suspend () -> T): Deferred<T> {
		return async(commonThreadPool) { func() }
	}

	override fun <T> asyncMap(func: suspend () -> T): Deferred<T> {
		return async(googleMapThreadPool) { func() }
	}
}
