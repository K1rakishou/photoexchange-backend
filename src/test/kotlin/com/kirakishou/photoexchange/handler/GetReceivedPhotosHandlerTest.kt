package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.received_photos.GetReceivedPhotosHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.received_photos.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import com.kirakishou.photoexchange.service.JsonConverterService
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
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
class GetReceivedPhotosHandlerTest : AbstractHandlerTest() {

	private fun getWebTestClient(jsonConverterService: JsonConverterService,
								 photoInfoRepository: PhotoInfoRepository): WebTestClient {
		val handler = GetReceivedPhotosHandler(jsonConverterService, photoInfoRepository)

		return WebTestClient.bindToRouterFunction(router {
			"/v1".nest {
				"/api".nest {
					accept(MediaType.APPLICATION_JSON).nest {
						GET("/get_received_photos/{user_id}/{photo_ids}", handler::handle)
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
	fun `should return received photos with receiver coordinates`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository)

		runBlocking {
			photoInfoDao.save(PhotoInfo(1, 1, -1L, "111", "222", "photo1", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(2, 2, -1L, "111", "222", "photo2", true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(3, 3, -1L, "111", "222", "photo3", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(4, 4, -1L, "111", "222", "photo4", true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(5, 5, -1L, "111", "222", "photo5", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(6, 1, -1L, "222", "111", "photo6", true, 22.2, 22.2, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(7, 2, -1L, "222", "111", "photo7", true, 22.2, 22.2, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(8, 3, -1L, "222", "111", "photo8", true, 22.2, 22.2, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(9, 4, -1L, "222", "111", "photo9", true, 22.2, 22.2, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(10, 5, -1L, "222", "111", "photo10", true, 22.2, 22.2, 6L)).awaitFirst()

			photoInfoExchangeDao.save(PhotoInfoExchange(1, 1, 6, "111", "222", 5)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(2, 2, 7, "111", "222", 6)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(3, 3, 8, "111", "222", 7)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(4, 4, 9, "111", "222", 8)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(5, 5, 10, "111", "222", 9)).awaitFirst()
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("v1/api/get_received_photos/111/6,7,8,9,10")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetReceivedPhotosResponse>(content)
			assertEquals(ErrorCode.GetReceivedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(5, response.receivedPhotos.size)

			assertEquals(10, response.receivedPhotos[0].photoId)
			assertEquals("photo5", response.receivedPhotos[0].uploadedPhotoName)
			assertEquals("photo10", response.receivedPhotos[0].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[0].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[0].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[0].uploadedPhotoName == response.receivedPhotos[0].receivedPhotoName)

			assertEquals(9, response.receivedPhotos[1].photoId)
			assertEquals("photo4", response.receivedPhotos[1].uploadedPhotoName)
			assertEquals("photo9", response.receivedPhotos[1].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[1].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[1].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[1].uploadedPhotoName == response.receivedPhotos[1].receivedPhotoName)

			assertEquals(8, response.receivedPhotos[2].photoId)
			assertEquals("photo3", response.receivedPhotos[2].uploadedPhotoName)
			assertEquals("photo8", response.receivedPhotos[2].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[2].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[2].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[2].uploadedPhotoName == response.receivedPhotos[2].receivedPhotoName)

			assertEquals(7, response.receivedPhotos[3].photoId)
			assertEquals("photo2", response.receivedPhotos[3].uploadedPhotoName)
			assertEquals("photo7", response.receivedPhotos[3].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[3].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[3].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[3].uploadedPhotoName == response.receivedPhotos[3].receivedPhotoName)

			assertEquals(6, response.receivedPhotos[4].photoId)
			assertEquals("photo1", response.receivedPhotos[4].uploadedPhotoName)
			assertEquals("photo6", response.receivedPhotos[4].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[4].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[4].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[4].uploadedPhotoName == response.receivedPhotos[4].receivedPhotoName)
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("v1/api/get_received_photos/222/1,2,3,4,5")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetReceivedPhotosResponse>(content)
			assertEquals(ErrorCode.GetReceivedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(5, response.receivedPhotos.size)

			assertEquals(5, response.receivedPhotos[0].photoId)
			assertEquals("photo10", response.receivedPhotos[0].uploadedPhotoName)
			assertEquals("photo5", response.receivedPhotos[0].receivedPhotoName)
			assertEquals(11.1, response.receivedPhotos[0].receiverLon, EPSILON)
			assertEquals(11.1, response.receivedPhotos[0].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[0].uploadedPhotoName == response.receivedPhotos[0].receivedPhotoName)

			assertEquals(4, response.receivedPhotos[1].photoId)
			assertEquals("photo9", response.receivedPhotos[1].uploadedPhotoName)
			assertEquals("photo4", response.receivedPhotos[1].receivedPhotoName)
			assertEquals(11.1, response.receivedPhotos[1].receiverLon, EPSILON)
			assertEquals(11.1, response.receivedPhotos[1].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[1].uploadedPhotoName == response.receivedPhotos[1].receivedPhotoName)

			assertEquals(3, response.receivedPhotos[2].photoId)
			assertEquals("photo8", response.receivedPhotos[2].uploadedPhotoName)
			assertEquals("photo3", response.receivedPhotos[2].receivedPhotoName)
			assertEquals(11.1, response.receivedPhotos[2].receiverLon, EPSILON)
			assertEquals(11.1, response.receivedPhotos[2].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[2].uploadedPhotoName == response.receivedPhotos[2].receivedPhotoName)

			assertEquals(2, response.receivedPhotos[3].photoId)
			assertEquals("photo7", response.receivedPhotos[3].uploadedPhotoName)
			assertEquals("photo2", response.receivedPhotos[3].receivedPhotoName)
			assertEquals(11.1, response.receivedPhotos[3].receiverLon, EPSILON)
			assertEquals(11.1, response.receivedPhotos[3].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[3].uploadedPhotoName == response.receivedPhotos[3].receivedPhotoName)

			assertEquals(1, response.receivedPhotos[4].photoId)
			assertEquals("photo6", response.receivedPhotos[4].uploadedPhotoName)
			assertEquals("photo1", response.receivedPhotos[4].receivedPhotoName)
			assertEquals(11.1, response.receivedPhotos[4].receiverLon, EPSILON)
			assertEquals(11.1, response.receivedPhotos[4].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[4].uploadedPhotoName == response.receivedPhotos[4].receivedPhotoName)
		}
	}
}