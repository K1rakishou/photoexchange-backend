package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.GetUploadedPhotosHandler
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import net.response.GetUploadedPhotosResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import kotlin.test.assertNotNull

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
				.uri("/v1/api/get_page_of_uploaded_photos/111/7/6")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetUploadedPhotosResponse>(content)
			assertEquals(ErrorCode.Ok.value, response.errorCode)
			assertEquals(6, response.uploadedPhotos.size)

      assertEquals(6, response.uploadedPhotos[0].photoId)
      assertEquals("photo6", response.uploadedPhotos[0].photoName)
      assertEquals(11.1, response.uploadedPhotos[0].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[0].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[0].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[0].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[0].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(5, response.uploadedPhotos[1].photoId)
      assertEquals("photo5", response.uploadedPhotos[1].photoName)
      assertEquals(11.1, response.uploadedPhotos[1].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[1].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[1].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[1].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[1].receiverInfoResponseData!!.receiverLat, EPSILON)

			assertEquals(4, response.uploadedPhotos[2].photoId)
			assertEquals("photo4", response.uploadedPhotos[2].photoName)
			assertEquals(11.1, response.uploadedPhotos[2].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[2].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[2].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[2].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[2].receiverInfoResponseData!!.receiverLat, EPSILON)

			assertEquals(3, response.uploadedPhotos[3].photoId)
			assertEquals("photo3", response.uploadedPhotos[3].photoName)
			assertEquals(11.1, response.uploadedPhotos[3].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[3].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[3].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[3].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[3].receiverInfoResponseData!!.receiverLat, EPSILON)

			assertEquals(2, response.uploadedPhotos[4].photoId)
			assertEquals("photo2", response.uploadedPhotos[4].photoName)
			assertEquals(11.1, response.uploadedPhotos[4].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[4].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[4].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[4].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[4].receiverInfoResponseData!!.receiverLat, EPSILON)

			assertEquals(1, response.uploadedPhotos[5].photoId)
			assertEquals("photo1", response.uploadedPhotos[5].photoName)
			assertEquals(11.1, response.uploadedPhotos[5].uploaderLon, EPSILON)
			assertEquals(11.1, response.uploadedPhotos[5].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[5].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[5].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[5].receiverInfoResponseData!!.receiverLat, EPSILON)
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("/v1/api/get_page_of_uploaded_photos/222/7/6")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetUploadedPhotosResponse>(content)
			assertEquals(ErrorCode.Ok.value, response.errorCode)
			assertEquals(6, response.uploadedPhotos.size)

      assertEquals(14, response.uploadedPhotos[0].photoId)
      assertEquals("photo14", response.uploadedPhotos[0].photoName)
      assertEquals(22.2, response.uploadedPhotos[0].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[0].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[0].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[0].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[0].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(13, response.uploadedPhotos[1].photoId)
      assertEquals("photo13", response.uploadedPhotos[1].photoName)
      assertEquals(22.2, response.uploadedPhotos[1].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[1].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[1].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[1].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[1].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(12, response.uploadedPhotos[2].photoId)
      assertEquals("photo12", response.uploadedPhotos[2].photoName)
      assertEquals(22.2, response.uploadedPhotos[2].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[2].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[2].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[2].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[2].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(11, response.uploadedPhotos[3].photoId)
      assertEquals("photo11", response.uploadedPhotos[3].photoName)
      assertEquals(22.2, response.uploadedPhotos[3].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[3].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[3].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[3].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[3].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(10, response.uploadedPhotos[4].photoId)
      assertEquals("photo10", response.uploadedPhotos[4].photoName)
      assertEquals(22.2, response.uploadedPhotos[4].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[4].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[4].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[4].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[4].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(9, response.uploadedPhotos[5].photoId)
      assertEquals("photo9", response.uploadedPhotos[5].photoName)
      assertEquals(22.2, response.uploadedPhotos[5].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[5].uploaderLat, EPSILON)
			assertNotNull(response.uploadedPhotos[5].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[5].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[5].receiverInfoResponseData!!.receiverLat, EPSILON)
		}
	}
}