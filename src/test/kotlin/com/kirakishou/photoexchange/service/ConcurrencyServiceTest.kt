package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import org.junit.Before
import org.junit.Test

class ConcurrencyServiceTest {

	private lateinit var concurrencyService: ConcurrencyService

	@Before
	fun setup() {
		concurrencyService = ConcurrencyService()
	}

	@Test
	fun `should return two threads`() {
		assert(concurrencyService.getThreadsCount(0.25, 1) == 2)
		assert(concurrencyService.getThreadsCount(0.25, 4) == 2)
	}

	@Test
	fun `should return three threads`() {
		assert(concurrencyService.getThreadsCount(0.25, 12) == 3)
		assert(concurrencyService.getThreadsCount(0.75, 4) == 3)
	}

	@Test
	fun `should return 12 threads`() {
		assert(concurrencyService.getThreadsCount(1.0, 12) == 12)
	}
}