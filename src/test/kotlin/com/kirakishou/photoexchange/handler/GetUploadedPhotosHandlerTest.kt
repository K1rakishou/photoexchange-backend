package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoExchangeRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.uploaded_photos.GetUploadedPhotosHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.uploaded_photos.GetUploadedPhotosResponse
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

@RunWith(SpringJUnit4ClassRunner::class)
class GetUploadedPhotosHandlerTest : AbstractHandlerTest() {

	private fun getWebTestClient(jsonConverterService: JsonConverterService,
								 photoInfoRepository: PhotoInfoRepository,
								 photoInfoExchangeRepository: PhotoInfoExchangeRepository,
								 concurrencyService: AbstractConcurrencyService): WebTestClient {
		val handler = GetUploadedPhotosHandler(jsonConverterService, photoInfoRepository,
			photoInfoExchangeRepository, concurrencyService)

		return WebTestClient.bindToRouterFunction(router {
			"/v1".nest {
				"/api".nest {
					accept(MediaType.APPLICATION_JSON).nest {
						GET("/get_uploaded_photos/{user_id}/{photo_ids}", handler::handle)
					}
				}
			}
		})
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
	fun `should return uploaded photos with uploader coordinates`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository, photoInfoExchangeRepository, concurrentService)

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
				.uri("v1/api/get_uploaded_photos/111/1,2,3,4,5")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetUploadedPhotosResponse>(content)
			assertEquals(ErrorCode.GetUploadedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(5, response.uploadedPhotos.size)

			assertEquals(5, response.uploadedPhotos[0].photoId)
			assertEquals("photo5", response.uploadedPhotos[0].photoName)
			assertEquals(11.1, response.uploadedPhotos[0].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[0].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[0].hasReceiverInfo)

			assertEquals(4, response.uploadedPhotos[1].photoId)
			assertEquals("photo4", response.uploadedPhotos[1].photoName)
			assertEquals(11.1, response.uploadedPhotos[1].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[1].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[1].hasReceiverInfo)

			assertEquals(3, response.uploadedPhotos[2].photoId)
			assertEquals("photo3", response.uploadedPhotos[2].photoName)
			assertEquals(11.1, response.uploadedPhotos[2].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[2].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[2].hasReceiverInfo)

			assertEquals(2, response.uploadedPhotos[3].photoId)
			assertEquals("photo2", response.uploadedPhotos[3].photoName)
			assertEquals(11.1, response.uploadedPhotos[3].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[3].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[3].hasReceiverInfo)

			assertEquals(1, response.uploadedPhotos[4].photoId)
			assertEquals("photo1", response.uploadedPhotos[4].photoName)
			assertEquals(11.1, response.uploadedPhotos[4].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[4].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[4].hasReceiverInfo)
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("v1/api/get_uploaded_photos/222/6,7,8,9,10")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetUploadedPhotosResponse>(content)
			assertEquals(ErrorCode.GetUploadedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(5, response.uploadedPhotos.size)

			assertEquals(10, response.uploadedPhotos[0].photoId)
			assertEquals("photo10", response.uploadedPhotos[0].photoName)
			assertEquals(22.2, response.uploadedPhotos[0].uploaderLon, EPSILON)
			assertEquals(22.2, response.uploadedPhotos[0].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[0].hasReceiverInfo)

			assertEquals(9, response.uploadedPhotos[1].photoId)
			assertEquals("photo9", response.uploadedPhotos[1].photoName)
			assertEquals(22.2, response.uploadedPhotos[1].uploaderLon, EPSILON)
			assertEquals(22.2, response.uploadedPhotos[1].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[1].hasReceiverInfo)

			assertEquals(8, response.uploadedPhotos[2].photoId)
			assertEquals("photo8", response.uploadedPhotos[2].photoName)
			assertEquals(22.2, response.uploadedPhotos[2].uploaderLon, EPSILON)
			assertEquals(22.2, response.uploadedPhotos[2].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[2].hasReceiverInfo)

			assertEquals(7, response.uploadedPhotos[3].photoId)
			assertEquals("photo7", response.uploadedPhotos[3].photoName)
			assertEquals(22.2, response.uploadedPhotos[3].uploaderLon, EPSILON)
			assertEquals(22.2, response.uploadedPhotos[3].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[3].hasReceiverInfo)

			assertEquals(6, response.uploadedPhotos[4].photoId)
			assertEquals("photo6", response.uploadedPhotos[4].photoName)
			assertEquals(22.2, response.uploadedPhotos[4].uploaderLon, EPSILON)
			assertEquals(22.2, response.uploadedPhotos[4].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[4].hasReceiverInfo)
		}
	}
}