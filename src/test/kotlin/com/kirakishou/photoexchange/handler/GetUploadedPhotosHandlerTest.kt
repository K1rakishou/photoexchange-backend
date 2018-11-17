package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.uploaded_photos.GetUploadedPhotosHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.uploaded_photos.GetUploadedPhotosResponse
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

@RunWith(SpringJUnit4ClassRunner::class)
class GetUploadedPhotosHandlerTest : AbstractHandlerTest() {

	private fun getWebTestClient(jsonConverterService: JsonConverterService,
								 photoInfoRepository: PhotoInfoRepository): WebTestClient {
		val handler = GetUploadedPhotosHandler(jsonConverterService, photoInfoRepository)

		return WebTestClient.bindToRouterFunction(router {
			"/v1".nest {
				"/api".nest {
					accept(MediaType.APPLICATION_JSON).nest {
						GET("/get_page_of_uploaded_photos/{user_id}/{last_uploaded_on}/{count}", handler::handle)
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
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository)

		runBlocking {
			photoInfoDao.save(PhotoInfo(1, 1, -1L, "111", "222", "photo1", true, 11.1, 11.1, 1L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(2, 2, -1L, "111", "222", "photo2", true, 11.1, 11.1, 2L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(3, 3, -1L, "111", "222", "photo3", true, 11.1, 11.1, 3L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(4, 4, -1L, "111", "222", "photo4", true, 11.1, 11.1, 4L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(5, 5, -1L, "111", "222", "photo5", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(6, 6, -1L, "111", "222", "photo6", true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(7, 7, -1L, "111", "222", "photo7", true, 11.1, 11.1, 7L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(8, 8, -1L, "111", "222", "photo8", true, 11.1, 11.1, 8L)).awaitFirst()

			photoInfoDao.save(PhotoInfo(9, 1, -1L, "222", "111", "photo9", true, 22.2, 22.2, 1L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(10, 2, -1L, "222", "111", "photo10", true, 22.2, 22.2, 2L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(11, 3, -1L, "222", "111", "photo11", true, 22.2, 22.2, 3L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(12, 4, -1L, "222", "111", "photo12", true, 22.2, 22.2, 4L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(13, 5, -1L, "222", "111", "photo13", true, 22.2, 22.2, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(14, 6, -1L, "222", "111", "photo14", true, 22.2, 22.2, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(15, 7, -1L, "222", "111", "photo15", true, 22.2, 22.2, 7L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(16, 8, -1L, "222", "111", "photo16", true, 22.2, 22.2, 8L)).awaitFirst()

			photoInfoExchangeDao.save(PhotoInfoExchange(1, 1, 9, "111", "222", 1)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(2, 2, 10, "111", "222", 2)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(3, 3, 11, "111", "222", 3)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(4, 4, 12, "111", "222", 4)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(5, 5, 13, "111", "222", 5)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(6, 6, 14, "111", "222", 6)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(7, 7, 15, "111", "222", 7)).awaitFirst()
			photoInfoExchangeDao.save(PhotoInfoExchange(8, 8, 16, "111", "222", 8)).awaitFirst()
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("/v1/api/get_page_of_uploaded_photos/111/7/6")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetUploadedPhotosResponse>(content)
			assertEquals(ErrorCode.GetUploadedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(6, response.uploadedPhotos.size)

      assertEquals(7, response.uploadedPhotos[0].photoId)
      assertEquals("photo7", response.uploadedPhotos[0].photoName)
      assertEquals(11.1, response.uploadedPhotos[0].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[0].uploaderLat, EPSILON)
      assertEquals(true, response.uploadedPhotos[0].hasReceiverInfo)

      assertEquals(6, response.uploadedPhotos[1].photoId)
      assertEquals("photo6", response.uploadedPhotos[1].photoName)
      assertEquals(11.1, response.uploadedPhotos[1].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[1].uploaderLat, EPSILON)
      assertEquals(true, response.uploadedPhotos[1].hasReceiverInfo)

			assertEquals(5, response.uploadedPhotos[2].photoId)
			assertEquals("photo5", response.uploadedPhotos[2].photoName)
			assertEquals(11.1, response.uploadedPhotos[2].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[2].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[2].hasReceiverInfo)

			assertEquals(4, response.uploadedPhotos[3].photoId)
			assertEquals("photo4", response.uploadedPhotos[3].photoName)
			assertEquals(11.1, response.uploadedPhotos[3].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[3].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[3].hasReceiverInfo)

			assertEquals(3, response.uploadedPhotos[4].photoId)
			assertEquals("photo3", response.uploadedPhotos[4].photoName)
			assertEquals(11.1, response.uploadedPhotos[4].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[4].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[4].hasReceiverInfo)

			assertEquals(2, response.uploadedPhotos[5].photoId)
			assertEquals("photo2", response.uploadedPhotos[5].photoName)
			assertEquals(11.1, response.uploadedPhotos[5].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[5].uploaderLat, EPSILON)
			assertEquals(true, response.uploadedPhotos[5].hasReceiverInfo)
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("/v1/api/get_page_of_uploaded_photos/222/7/6")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetUploadedPhotosResponse>(content)
			assertEquals(ErrorCode.GetUploadedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(6, response.uploadedPhotos.size)

      assertEquals(15, response.uploadedPhotos[0].photoId)
      assertEquals("photo15", response.uploadedPhotos[0].photoName)
      assertEquals(22.2, response.uploadedPhotos[0].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[0].uploaderLat, EPSILON)
      assertEquals(true, response.uploadedPhotos[0].hasReceiverInfo)

      assertEquals(14, response.uploadedPhotos[1].photoId)
      assertEquals("photo14", response.uploadedPhotos[1].photoName)
      assertEquals(22.2, response.uploadedPhotos[1].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[1].uploaderLat, EPSILON)
      assertEquals(true, response.uploadedPhotos[1].hasReceiverInfo)

      assertEquals(13, response.uploadedPhotos[2].photoId)
      assertEquals("photo13", response.uploadedPhotos[2].photoName)
      assertEquals(22.2, response.uploadedPhotos[2].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[2].uploaderLat, EPSILON)
      assertEquals(true, response.uploadedPhotos[2].hasReceiverInfo)

      assertEquals(12, response.uploadedPhotos[3].photoId)
      assertEquals("photo12", response.uploadedPhotos[3].photoName)
      assertEquals(22.2, response.uploadedPhotos[3].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[3].uploaderLat, EPSILON)
      assertEquals(true, response.uploadedPhotos[3].hasReceiverInfo)

      assertEquals(11, response.uploadedPhotos[4].photoId)
      assertEquals("photo11", response.uploadedPhotos[4].photoName)
      assertEquals(22.2, response.uploadedPhotos[4].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[4].uploaderLat, EPSILON)
      assertEquals(true, response.uploadedPhotos[4].hasReceiverInfo)

      assertEquals(10, response.uploadedPhotos[5].photoId)
      assertEquals("photo10", response.uploadedPhotos[5].photoName)
      assertEquals(22.2, response.uploadedPhotos[5].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[5].uploaderLat, EPSILON)
      assertEquals(true, response.uploadedPhotos[5].hasReceiverInfo)
		}
	}
}