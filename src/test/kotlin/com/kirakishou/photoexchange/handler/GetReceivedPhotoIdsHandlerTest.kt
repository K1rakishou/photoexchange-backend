package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.received_photos.GetReceivedPhotoIdsHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.received_photos.GetReceivedPhotoIdsResponse
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.JsonConverterService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import java.time.Duration

@RunWith(SpringJUnit4ClassRunner::class)
class GetReceivedPhotoIdsHandlerTest : AbstractHandlerTest() {

	private fun getWebTestClient(jsonConverterService: JsonConverterService,
								 photoInfoRepository: PhotoInfoRepository): WebTestClient {
		val handler = GetReceivedPhotoIdsHandler(jsonConverterService, photoInfoRepository)

		return WebTestClient.bindToRouterFunction(router {
			"/v1".nest {
				"/api".nest {
					accept(MediaType.APPLICATION_JSON).nest {
						GET("/get_received_photo_ids/{user_id}/{last_id}/{count}", handler::handle)
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
	fun `should only return photos that have location map`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository)

		runBlocking {
			photoInfoDao.save(PhotoInfo(1, 1, -1L, "111", "222", "photo1", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(2, 2, -1L, "111", "222", "photo2", true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(3, 3, -1L, "111", "222", "photo3", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(4, 4, 1L, "111", "222", "photo4", true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(5, 5, -1L, "111", "222", "photo5", true, 11.1, 11.1, 5L)).awaitFirst()
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("v1/api/get_received_photo_ids/222/10000/5")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetReceivedPhotoIdsResponse>(content)
			assertEquals(ErrorCode.GetReceivedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(1, response.receivedPhotoIds.size)

			assertEquals(4, response.receivedPhotoIds[0])
		}
	}

	@Test
	fun `photo ids should be sorted by photo id in descending order`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository)

		runBlocking {
			photoInfoDao.save(PhotoInfo(1, 1, 2L, "111", "222", "photo1", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(2, 2, 5L, "111", "222", "photo2", true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(3, 3, 3L, "111", "222", "photo3", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(4, 4, 1L, "111", "222", "photo4", true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(5, 5, 4L, "111", "222", "photo5", true, 11.1, 11.1, 5L)).awaitFirst()
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("v1/api/get_received_photo_ids/222/10000/5")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetReceivedPhotoIdsResponse>(content)
			assertEquals(ErrorCode.GetReceivedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(5, response.receivedPhotoIds.size)

			assertEquals(5, response.receivedPhotoIds[0])
			assertEquals(4, response.receivedPhotoIds[1])
			assertEquals(3, response.receivedPhotoIds[2])
			assertEquals(2, response.receivedPhotoIds[3])
			assertEquals(1, response.receivedPhotoIds[4])
		}
	}
}