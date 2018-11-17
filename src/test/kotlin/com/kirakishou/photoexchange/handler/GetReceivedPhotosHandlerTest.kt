package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.GetReceivedPhotosHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.received_photos.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.model.repo.PhotoInfo
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
						GET("/get_page_of_received_photos/{user_id}/{last_uploaded_on}/{count}", handler::handle)
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
      photoInfoDao.save(PhotoInfo(1, 9, -1L, "111", "photo1", true, 11.1, 11.1, 1L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(2, 10, -1L, "111", "photo2", true, 11.1, 11.1, 2L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(3, 11, -1L, "111", "photo3", true, 11.1, 11.1, 3L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(4, 12, -1L, "111", "photo4", true, 11.1, 11.1, 4L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(5, 13, -1L, "111", "photo5", true, 11.1, 11.1, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(6, 14, -1L, "111", "photo6", true, 11.1, 11.1, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(7, 15, -1L, "111", "photo7", true, 11.1, 11.1, 7L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(8, 16, -1L, "111", "photo8", true, 11.1, 11.1, 8L)).awaitFirst()

      photoInfoDao.save(PhotoInfo(9, 1, -1L, "222", "photo9", true, 22.2, 22.2, 1L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(10, 2, -1L, "222", "photo10", true, 22.2, 22.2, 2L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(11, 3, -1L, "222", "photo11", true, 22.2, 22.2, 3L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(12, 4, -1L, "222", "photo12", true, 22.2, 22.2, 4L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(13, 5, -1L, "222", "photo13", true, 22.2, 22.2, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(14, 6, -1L, "222", "photo14", true, 22.2, 22.2, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(15, 7, -1L, "222", "photo15", true, 22.2, 22.2, 7L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(16, 8, -1L, "222", "photo16", true, 22.2, 22.2, 8L)).awaitFirst()
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("/v1/api/get_page_of_received_photos/111/8/6")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetReceivedPhotosResponse>(content)
			assertEquals(ErrorCode.GetReceivedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(6, response.receivedPhotos.size)

			assertEquals(16, response.receivedPhotos[0].photoId)
			assertEquals("photo8", response.receivedPhotos[0].uploadedPhotoName)
			assertEquals("photo16", response.receivedPhotos[0].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[0].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[0].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[0].uploadedPhotoName == response.receivedPhotos[0].receivedPhotoName)

			assertEquals(15, response.receivedPhotos[1].photoId)
			assertEquals("photo7", response.receivedPhotos[1].uploadedPhotoName)
			assertEquals("photo15", response.receivedPhotos[1].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[1].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[1].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[1].uploadedPhotoName == response.receivedPhotos[1].receivedPhotoName)

			assertEquals(14, response.receivedPhotos[2].photoId)
			assertEquals("photo6", response.receivedPhotos[2].uploadedPhotoName)
			assertEquals("photo14", response.receivedPhotos[2].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[2].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[2].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[2].uploadedPhotoName == response.receivedPhotos[2].receivedPhotoName)

			assertEquals(13, response.receivedPhotos[3].photoId)
			assertEquals("photo5", response.receivedPhotos[3].uploadedPhotoName)
			assertEquals("photo13", response.receivedPhotos[3].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[3].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[3].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[3].uploadedPhotoName == response.receivedPhotos[3].receivedPhotoName)

			assertEquals(12, response.receivedPhotos[4].photoId)
			assertEquals("photo4", response.receivedPhotos[4].uploadedPhotoName)
			assertEquals("photo12", response.receivedPhotos[4].receivedPhotoName)
			assertEquals(22.2, response.receivedPhotos[4].receiverLon, EPSILON)
			assertEquals(22.2, response.receivedPhotos[4].receiverLat, EPSILON)
			assertEquals(false, response.receivedPhotos[4].uploadedPhotoName == response.receivedPhotos[4].receivedPhotoName)

      assertEquals(11, response.receivedPhotos[5].photoId)
      assertEquals("photo3", response.receivedPhotos[5].uploadedPhotoName)
      assertEquals("photo11", response.receivedPhotos[5].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[5].receiverLon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[5].receiverLat, EPSILON)
      assertEquals(false, response.receivedPhotos[5].uploadedPhotoName == response.receivedPhotos[5].receivedPhotoName)
		}

		kotlin.run {
			val content = webClient
				.get()
        .uri("/v1/api/get_page_of_received_photos/222/8/6")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetReceivedPhotosResponse>(content)
			assertEquals(ErrorCode.GetReceivedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(6, response.receivedPhotos.size)

      assertEquals(8, response.receivedPhotos[0].photoId)
      assertEquals("photo16", response.receivedPhotos[0].uploadedPhotoName)
      assertEquals("photo8", response.receivedPhotos[0].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[0].receiverLon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[0].receiverLat, EPSILON)
      assertEquals(false, response.receivedPhotos[0].uploadedPhotoName == response.receivedPhotos[0].receivedPhotoName)

      assertEquals(7, response.receivedPhotos[1].photoId)
      assertEquals("photo15", response.receivedPhotos[1].uploadedPhotoName)
      assertEquals("photo7", response.receivedPhotos[1].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[1].receiverLon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[1].receiverLat, EPSILON)
      assertEquals(false, response.receivedPhotos[1].uploadedPhotoName == response.receivedPhotos[1].receivedPhotoName)

      assertEquals(6, response.receivedPhotos[2].photoId)
      assertEquals("photo14", response.receivedPhotos[2].uploadedPhotoName)
      assertEquals("photo6", response.receivedPhotos[2].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[2].receiverLon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[2].receiverLat, EPSILON)
      assertEquals(false, response.receivedPhotos[2].uploadedPhotoName == response.receivedPhotos[2].receivedPhotoName)

      assertEquals(5, response.receivedPhotos[3].photoId)
      assertEquals("photo13", response.receivedPhotos[3].uploadedPhotoName)
      assertEquals("photo5", response.receivedPhotos[3].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[3].receiverLon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[3].receiverLat, EPSILON)
      assertEquals(false, response.receivedPhotos[3].uploadedPhotoName == response.receivedPhotos[3].receivedPhotoName)

      assertEquals(4, response.receivedPhotos[4].photoId)
      assertEquals("photo12", response.receivedPhotos[4].uploadedPhotoName)
      assertEquals("photo4", response.receivedPhotos[4].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[4].receiverLon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[4].receiverLat, EPSILON)
      assertEquals(false, response.receivedPhotos[4].uploadedPhotoName == response.receivedPhotos[4].receivedPhotoName)

      assertEquals(3, response.receivedPhotos[5].photoId)
      assertEquals("photo11", response.receivedPhotos[5].uploadedPhotoName)
      assertEquals("photo3", response.receivedPhotos[5].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[5].receiverLon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[5].receiverLat, EPSILON)
      assertEquals(false, response.receivedPhotos[5].uploadedPhotoName == response.receivedPhotos[5].receivedPhotoName)
		}
	}
}