package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.UploadPhotoHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.request.SendPhotoPacket
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.StaticMapDownloaderService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.router

@RunWith(SpringJUnit4ClassRunner::class)
class UploadPhotoHandlerTest : AbstractHandlerTest() {

	private fun getWebTestClient(jsonConverterService: JsonConverterService,
								 photoInfoRepository: PhotoInfoRepository,
								 staticMapDownloaderService: StaticMapDownloaderService,
								 concurrencyService: AbstractConcurrencyService): WebTestClient {
		val handler = UploadPhotoHandler(
			jsonConverterService,
			photoInfoRepository,
			staticMapDownloaderService,
			concurrencyService
		)

		return WebTestClient.bindToRouterFunction(router {
			"/v1".nest {
				"/api".nest {
					accept(MediaType.MULTIPART_FORM_DATA).nest {
						POST("/upload", handler::handle)
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
	fun `test should exchange two photos`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository, staticMapDownloaderService, concurrentService)

		runBlocking {
			Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
		}

		kotlin.run {
			val packet = SendPhotoPacket(33.4, 55.2, "111", true)
			val multipartData = createTestMultipartFile(PHOTO1, packet)

			val content = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<UploadPhotoResponse>(content)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response.errorCode)

			val photoInfo = runBlocking {
				photoInfoDao.findById(1).awaitFirst()
			}

			assertEquals(1, photoInfo.exchangeId)
			assertEquals(packet.userId, photoInfo.uploaderUserId)
			assertEquals("", photoInfo.receiverUserId)
			assertEquals(packet.isPublic, photoInfo.isPublic)
			assertEquals(packet.lon, photoInfo.lon, EPSILON)
			assertEquals(packet.lat, photoInfo.lat, EPSILON)

			val photoInfoExchange = runBlocking {
				photoInfoExchangeDao.findById(1).awaitFirst()
			}

			assertEquals("111", photoInfoExchange.uploaderUserId)
			assertEquals("", photoInfoExchange.receiverUserId)
			assertEquals(1, photoInfoExchange.uploaderPhotoId)
			assertEquals(-1, photoInfoExchange.receiverPhotoId)
		}

		kotlin.run {
			val packet = SendPhotoPacket(11.4, 24.45, "222", true)
			val multipartData = createTestMultipartFile(PHOTO2, packet)

			val content = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<UploadPhotoResponse>(content)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response.errorCode)

			val photoInfo1 = runBlocking {
				photoInfoDao.findById(1).awaitFirst()
			}

			val photoInfo2 = runBlocking {
				photoInfoDao.findById(2).awaitFirst()
			}

			assertEquals(1, photoInfo1.exchangeId)
			assertEquals("111", photoInfo1.uploaderUserId)
			assertEquals("222", photoInfo1.receiverUserId)
			assertEquals(true, photoInfo1.isPublic)
			assertEquals(33.4, photoInfo1.lon, EPSILON)
			assertEquals(55.2, photoInfo1.lat, EPSILON)

			assertEquals(1, photoInfo1.exchangeId)
			assertEquals("222", photoInfo2.uploaderUserId)
			assertEquals("111", photoInfo2.receiverUserId)
			assertEquals(true, photoInfo2.isPublic)
			assertEquals(11.4, photoInfo2.lon, EPSILON)
			assertEquals(24.45, photoInfo2.lat, EPSILON)

			val photoInfoExchange = runBlocking {
				photoInfoExchangeDao.findById(1).awaitFirst()
			}

			assertEquals(1, photoInfoExchange.uploaderPhotoId)
			assertEquals(2, photoInfoExchange.receiverPhotoId)
			assertEquals("111", photoInfoExchange.uploaderUserId)
			assertEquals("222", photoInfoExchange.receiverUserId)
		}
	}

	@Test
	fun `test should not exchange two photos with the same user id`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository, staticMapDownloaderService, concurrentService)

		runBlocking {
			Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
		}

		kotlin.run {
			val packet = SendPhotoPacket(33.4, 55.2, "111", true)
			val multipartData = createTestMultipartFile(PHOTO1, packet)

			val content = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<UploadPhotoResponse>(content)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response.errorCode)

			val photoInfo = runBlocking {
				photoInfoDao.findById(1).awaitFirst()
			}

			assertEquals(1, photoInfo.exchangeId)
			assertEquals(packet.userId, photoInfo.uploaderUserId)
			assertEquals("", photoInfo.receiverUserId)
			assertEquals(packet.isPublic, photoInfo.isPublic)
			assertEquals(packet.lon, photoInfo.lon, EPSILON)
			assertEquals(packet.lat, photoInfo.lat, EPSILON)

			val photoInfoExchange = runBlocking {
				photoInfoExchangeDao.findById(1).awaitFirst()
			}

			assertEquals("111", photoInfoExchange.uploaderUserId)
			assertEquals("", photoInfoExchange.receiverUserId)
			assertEquals(1, photoInfoExchange.uploaderPhotoId)
			assertEquals(-1, photoInfoExchange.receiverPhotoId)
		}

		kotlin.run {
			val packet = SendPhotoPacket(11.4, 24.45, "111", true)
			val multipartData = createTestMultipartFile(PHOTO2, packet)

			val content = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<UploadPhotoResponse>(content)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response.errorCode)

			val photoInfo1 = runBlocking {
				photoInfoDao.findById(1).awaitFirst()
			}

			val photoInfo2 = runBlocking {
				photoInfoDao.findById(2).awaitFirst()
			}

			assertEquals(1, photoInfo1.exchangeId)
			assertEquals("111", photoInfo1.uploaderUserId)
			assertEquals("", photoInfo1.receiverUserId)
			assertEquals(true, photoInfo1.isPublic)
			assertEquals(33.4, photoInfo1.lon, EPSILON)
			assertEquals(55.2, photoInfo1.lat, EPSILON)

			assertEquals(2, photoInfo2.exchangeId)
			assertEquals("111", photoInfo2.uploaderUserId)
			assertEquals("", photoInfo2.receiverUserId)
			assertEquals(true, photoInfo2.isPublic)
			assertEquals(11.4, photoInfo2.lon, EPSILON)
			assertEquals(24.45, photoInfo2.lat, EPSILON)

			val photoInfoExchange1 = runBlocking {
				photoInfoExchangeDao.findById(1).awaitFirst()
			}

			val photoInfoExchange2 = runBlocking {
				photoInfoExchangeDao.findById(2).awaitFirst()
			}

			assertEquals("111", photoInfoExchange1.uploaderUserId)
			assertEquals("", photoInfoExchange1.receiverUserId)
			assertEquals(1, photoInfoExchange1.uploaderPhotoId)
			assertEquals(-1, photoInfoExchange1.receiverPhotoId)

			assertEquals("111", photoInfoExchange2.uploaderUserId)
			assertEquals("", photoInfoExchange2.receiverUserId)
			assertEquals(2, photoInfoExchange2.uploaderPhotoId)
			assertEquals(-1, photoInfoExchange2.receiverPhotoId)
		}
	}

	@Test
	fun `test should exchange 4 photos`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository, staticMapDownloaderService, concurrentService)

		runBlocking {
			Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
		}

		kotlin.run {
			val packet = SendPhotoPacket(33.4, 55.2, "111", true)
			val multipartData = createTestMultipartFile(PHOTO1, packet)

			val content = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<UploadPhotoResponse>(content)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response.errorCode)

			val photoInfo = runBlocking {
				photoInfoDao.findById(1).awaitFirst()
			}

			assertEquals(1, photoInfo.exchangeId)
			assertEquals(packet.userId, photoInfo.uploaderUserId)
			assertEquals("", photoInfo.receiverUserId)
			assertEquals(packet.isPublic, photoInfo.isPublic)
			assertEquals(packet.lon, photoInfo.lon, EPSILON)
			assertEquals(packet.lat, photoInfo.lat, EPSILON)

			val photoInfoExchange = runBlocking {
				photoInfoExchangeDao.findById(1).awaitFirst()
			}

			assertEquals("111", photoInfoExchange.uploaderUserId)
			assertEquals("", photoInfoExchange.receiverUserId)
			assertEquals(1, photoInfoExchange.uploaderPhotoId)
			assertEquals(-1, photoInfoExchange.receiverPhotoId)
		}

		kotlin.run {
			val packet = SendPhotoPacket(11.4, 24.45, "222", true)
			val multipartData = createTestMultipartFile(PHOTO2, packet)

			val content = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<UploadPhotoResponse>(content)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response.errorCode)

			val photoInfo1 = runBlocking {
				photoInfoDao.findById(1).awaitFirst()
			}

			val photoInfo2 = runBlocking {
				photoInfoDao.findById(2).awaitFirst()
			}

			assertEquals(1, photoInfo1.exchangeId)
			assertEquals("111", photoInfo1.uploaderUserId)
			assertEquals("222", photoInfo1.receiverUserId)
			assertEquals(true, photoInfo1.isPublic)
			assertEquals(33.4, photoInfo1.lon, EPSILON)
			assertEquals(55.2, photoInfo1.lat, EPSILON)

			assertEquals(1, photoInfo2.exchangeId)
			assertEquals("222", photoInfo2.uploaderUserId)
			assertEquals("111", photoInfo2.receiverUserId)
			assertEquals(true, photoInfo2.isPublic)
			assertEquals(11.4, photoInfo2.lon, EPSILON)
			assertEquals(24.45, photoInfo2.lat, EPSILON)

			val photoInfoExchange1 = runBlocking {
				photoInfoExchangeDao.findById(1).awaitFirst()
			}

			val photoInfoExchange2 = runBlocking {
				photoInfoExchangeDao.findById(2).awaitFirst()
			}

			assertEquals("111", photoInfoExchange1.uploaderUserId)
			assertEquals("222", photoInfoExchange1.receiverUserId)
			assertEquals(1, photoInfoExchange1.uploaderPhotoId)
			assertEquals(2, photoInfoExchange1.receiverPhotoId)

			assertEquals(true, photoInfoExchange2.isEmpty())
		}

		kotlin.run {
			val packet3 = SendPhotoPacket(36.4, 66.66, "333", true)
			val multipartData3 = createTestMultipartFile(PHOTO3, packet3)

			val packet4 = SendPhotoPacket(38.4235, 16.7788, "444", true)
			val multipartData4 = createTestMultipartFile(PHOTO4, packet4)

			val content1 = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData3))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response1 = fromBodyContent<UploadPhotoResponse>(content1)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response1.errorCode)

			val content2 = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData4))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response2 = fromBodyContent<UploadPhotoResponse>(content2)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response2.errorCode)

			val photoInfo1 = runBlocking {
				photoInfoDao.findById(1).awaitFirst()
			}

			val photoInfo2 = runBlocking {
				photoInfoDao.findById(2).awaitFirst()
			}

			val photoInfo3 = runBlocking {
				photoInfoDao.findById(3).awaitFirst()
			}

			val photoInfo4 = runBlocking {
				photoInfoDao.findById(4).awaitFirst()
			}

			assertEquals(1, photoInfo1.exchangeId)
			assertEquals("111", photoInfo1.uploaderUserId)
			assertEquals("222", photoInfo1.receiverUserId)
			assertEquals(true, photoInfo1.isPublic)
			assertEquals(33.4, photoInfo1.lon, EPSILON)
			assertEquals(55.2, photoInfo1.lat, EPSILON)

			assertEquals(1, photoInfo2.exchangeId)
			assertEquals("222", photoInfo2.uploaderUserId)
			assertEquals("111", photoInfo2.receiverUserId)
			assertEquals(true, photoInfo2.isPublic)
			assertEquals(11.4, photoInfo2.lon, EPSILON)
			assertEquals(24.45, photoInfo2.lat, EPSILON)

			assertEquals(2, photoInfo3.exchangeId)
			assertEquals("333", photoInfo3.uploaderUserId)
			assertEquals("444", photoInfo3.receiverUserId)
			assertEquals(true, photoInfo3.isPublic)
			assertEquals(36.4, photoInfo3.lon, EPSILON)
			assertEquals(66.66, photoInfo3.lat, EPSILON)

			assertEquals(2, photoInfo4.exchangeId)
			assertEquals("444", photoInfo4.uploaderUserId)
			assertEquals("333", photoInfo4.receiverUserId)
			assertEquals(true, photoInfo4.isPublic)
			assertEquals(38.4235, photoInfo4.lon, EPSILON)
			assertEquals(16.7788, photoInfo4.lat, EPSILON)

			val photoInfoExchange1 = runBlocking {
				photoInfoExchangeDao.findById(1).awaitFirst()
			}

			val photoInfoExchange2 = runBlocking {
				photoInfoExchangeDao.findById(2).awaitFirst()
			}

			val photoInfoExchange3 = runBlocking {
				photoInfoExchangeDao.findById(3).awaitFirst()
			}

			val photoInfoExchange4 = runBlocking {
				photoInfoExchangeDao.findById(4).awaitFirst()
			}

			assertEquals("111", photoInfoExchange1.uploaderUserId)
			assertEquals("222", photoInfoExchange1.receiverUserId)
			assertEquals(1, photoInfoExchange1.uploaderPhotoId)
			assertEquals(2, photoInfoExchange1.receiverPhotoId)

			assertEquals("333", photoInfoExchange2.uploaderUserId)
			assertEquals("444", photoInfoExchange2.receiverUserId)
			assertEquals(3, photoInfoExchange2.uploaderPhotoId)
			assertEquals(4, photoInfoExchange2.receiverPhotoId)

			assertEquals(true, photoInfoExchange3.isEmpty())
			assertEquals(true, photoInfoExchange4.isEmpty())
		}
	}
}