package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.ReceivePhotosHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.ReceivePhotosResponse
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import java.time.Duration

@RunWith(SpringJUnit4ClassRunner::class)
class ReceivePhotosHandlerTest  : AbstractHandlerTest() {

	private fun getWebTestClient(jsonConverterService: JsonConverterService,
								 photoInfoRepository: PhotoInfoRepository,
								 concurrencyService: AbstractConcurrencyService): WebTestClient {
		val handler = ReceivePhotosHandler(jsonConverterService, photoInfoRepository, concurrencyService)

		return WebTestClient.bindToRouterFunction(router {
			"/v1".nest {
				"/api".nest {
					accept(MediaType.APPLICATION_JSON).nest {
						GET("/receive_photos/{photo_names}/{user_id}", handler::handle)
					}
				}
			}
		})
			.configureClient().responseTimeout(Duration.ofMillis(1_000_000))
			.build()
	}

	@Before
	fun setUp() {
		super.init()
	}

	@After
	fun tearDown() {
		super.clear()
	}

	@Test
	fun `should return uploaded photo name and exchanged photo name list with receiver coordinates`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository, concurrentService)

		runBlocking {
			photoInfoDao.save(PhotoInfo(1, 1, "111", "222", "photo1", true, false, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(2, 2, "111", "222", "photo2", true, false, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(3, 3, "111", "222", "photo3", true, true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(4, 4, "111", "222", "photo4", true, true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(5, 5, "111", "222", "photo5", true, true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(6, 1, "222", "111", "photo6", true, true, 22.2, 22.2, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(7, 2, "222", "111", "photo7", true, true, 22.2, 22.2, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(8, 3, "222", "111", "photo8", true, true, 22.2, 22.2, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(9, 4, "222", "111", "photo9", true, false, 22.2, 22.2, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(10, 5, "222", "111", "photo10", true, false, 22.2, 22.2, 6L)).awaitFirst()

			photoInfoExchangeDao.save(PhotoInfoExchange(1, 1, 6, "111", "222", 5)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(2, 2, 7, "111", "222", 6)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(3, 3, 8, "111", "222", 7)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(4, 4, 9, "111", "222", 8)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(5, 5, 10, "111", "222", 9)).awaitFirst()
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("v1/api/receive_photos/photo1,photo2,photo3,photo4,photo5/111")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<ReceivePhotosResponse>(content)
			assertEquals(ErrorCode.ReceivePhotosErrors.Ok.value, response.errorCode)
			assertEquals(3, response.receivedPhotos.size)

			assertEquals(8, response.receivedPhotos[0].photoId)
			assertEquals("photo3", response.receivedPhotos[0].uploadedPhotoName)
			assertEquals("photo8", response.receivedPhotos[0].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[0].lon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[0].lat, EPSILON)

			assertEquals(9, response.receivedPhotos[1].photoId)
			assertEquals("photo4", response.receivedPhotos[1].uploadedPhotoName)
			assertEquals("photo9", response.receivedPhotos[1].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[1].lon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[1].lat, EPSILON)

			assertEquals(10, response.receivedPhotos[2].photoId)
			assertEquals("photo5", response.receivedPhotos[2].uploadedPhotoName)
			assertEquals("photo10", response.receivedPhotos[2].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[2].lon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[2].lat, EPSILON)
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("v1/api/receive_photos/photo6,photo7,photo8,photo9,photo10/222")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<ReceivePhotosResponse>(content)
			assertEquals(ErrorCode.ReceivePhotosErrors.Ok.value, response.errorCode)
			assertEquals(3, response.receivedPhotos.size)

			assertEquals(1, response.receivedPhotos[0].photoId)
			assertEquals("photo6", response.receivedPhotos[0].uploadedPhotoName)
			assertEquals("photo1", response.receivedPhotos[0].receivedPhotoName)
			assertEquals(11.1, response.receivedPhotos[0].lon, EPSILON)
			assertEquals(11.1, response.receivedPhotos[0].lat, EPSILON)

			assertEquals(2, response.receivedPhotos[1].photoId)
			assertEquals("photo7", response.receivedPhotos[1].uploadedPhotoName)
			assertEquals("photo2", response.receivedPhotos[1].receivedPhotoName)
			assertEquals(11.1, response.receivedPhotos[1].lon, EPSILON)
			assertEquals(11.1, response.receivedPhotos[1].lat, EPSILON)

			assertEquals(3, response.receivedPhotos[2].photoId)
			assertEquals("photo8", response.receivedPhotos[2].uploadedPhotoName)
			assertEquals("photo3", response.receivedPhotos[2].receivedPhotoName)
			assertEquals(11.1, response.receivedPhotos[2].lon, EPSILON)
			assertEquals(11.1, response.receivedPhotos[2].lat, EPSILON)
		}
	}
}