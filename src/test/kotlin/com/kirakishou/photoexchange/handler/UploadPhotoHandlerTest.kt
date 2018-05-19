package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.handlers.UploadPhotoHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.request.SendPhotoPacket
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.service.GeneratorService
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import com.kirakishou.photoexchange.service.concurrency.TestConcurrencyService
import com.mongodb.MongoClient
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.router
import java.time.Duration

@RunWith(SpringRunner::class)
class UploadPhotoHandlerTest : AbstractHandlerTest() {

	lateinit var template: MongoTemplate

	lateinit var concurrentService: AbstractConcurrencyService
	lateinit var jsonConverterService: JsonConverterService

	lateinit var mongoSequenceDao: MongoSequenceDao
	lateinit var photoInfoDao: PhotoInfoDao
	lateinit var photoInfoExchangeDao: PhotoInfoExchangeDao
	lateinit var galleryPhotoDao: GalleryPhotoDao
	lateinit var favouritedPhotoDao: FavouritedPhotoDao
	lateinit var reportedPhotoDao: ReportedPhotoDao
	lateinit var userInfoDao: UserInfoDao

	lateinit var photoInfoRepository: PhotoInfoRepository

	private fun getWebTestClient(jsonConverterService: JsonConverterService,
								 photoInfoRepository: PhotoInfoRepository,
								 concurrencyService: AbstractConcurrencyService): WebTestClient {
		val uploadPhotoHandler = UploadPhotoHandler(jsonConverterService, photoInfoRepository, concurrencyService)

		return WebTestClient.bindToRouterFunction(router {
			"/v1".nest {
				"/api".nest {
					accept(MediaType.MULTIPART_FORM_DATA).nest {
						POST("/upload", uploadPhotoHandler::handle)
					}
				}
			}
		})
			.configureClient().responseTimeout(Duration.ofMillis(1_000_000))
			.build()
	}

	@Before
	fun setUp() {
		template = MongoTemplate(SimpleMongoDbFactory(MongoClient(ServerSettings.DatabaseInfo.HOST, ServerSettings.DatabaseInfo.PORT),
			"photoexchange_test"))

		val generator = GeneratorService()

		concurrentService = TestConcurrencyService()
		jsonConverterService = JsonConverterService(gson)

		mongoSequenceDao = MongoSequenceDao(template).also { it.create() }
		photoInfoDao = PhotoInfoDao(template).also { it.create() }
		photoInfoExchangeDao = PhotoInfoExchangeDao(template).also { it.create() }
		galleryPhotoDao = GalleryPhotoDao(template).also { it.create() }
		favouritedPhotoDao = FavouritedPhotoDao(template).also { it.create() }
		reportedPhotoDao = ReportedPhotoDao(template).also { it.create() }
		userInfoDao = UserInfoDao(template).also { it.create() }

		val userInfoRepository = UserInfoRepository(mongoSequenceDao, userInfoDao, generator, concurrentService)

		photoInfoRepository = PhotoInfoRepository(
			mongoSequenceDao,
			photoInfoDao,
			photoInfoExchangeDao,
			galleryPhotoDao,
			favouritedPhotoDao,
			reportedPhotoDao,
			userInfoRepository,
			generator,
			concurrentService
		)
	}

	@After
	fun tearDown() {
		mongoSequenceDao.clear()
		photoInfoDao.clear()
		photoInfoExchangeDao.clear()
		galleryPhotoDao.clear()
		favouritedPhotoDao.clear()
		reportedPhotoDao.clear()
		userInfoDao.clear()
	}

	fun createTestMultipartFile(fileResourceName: String, packet: SendPhotoPacket): MultiValueMap<String, Any> {
		val fileResource = ClassPathResource(fileResourceName)

		val photoPart = HttpEntity(fileResource, HttpHeaders().also { it.contentType = MediaType.IMAGE_JPEG })
		val packetPart = HttpEntity(jsonConverterService.toJson(packet), HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON })

		val parts = LinkedMultiValueMap<String, Any>()
		parts.add("photo", photoPart)
		parts.add("packet", packetPart)

		return parts
	}

	@Test
	fun `test should exchange two photos`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository, concurrentService)

		val packet1 = SendPhotoPacket(33.4, 55.2, "111", true)
		val multipartData1 = createTestMultipartFile("test_photos/photo_1.jpg", packet1)

		val packet2 = SendPhotoPacket(11.4, 24.45, "222", true)
		val multipartData2 = createTestMultipartFile("test_photos/photo_2.jpg", packet2)

		kotlin.run {
			val content = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData1))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<UploadPhotoResponse>(content)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response.errorCode)

			val photoInfo = runBlocking {
				photoInfoDao.findById(1)
			}

			assertEquals(1, photoInfo.exchangeId)
			assertEquals(packet1.userId, photoInfo.uploaderUserId)
			assertEquals("", photoInfo.receiverUserId)
			assertEquals(packet1.isPublic, photoInfo.isPublic)
			assertEquals(packet1.lon, photoInfo.lon, EPSILON)
			assertEquals(packet1.lat, photoInfo.lat, EPSILON)

			val photoInfoExchange = runBlocking {
				photoInfoExchangeDao.findById(1)
			}

			assertEquals(1, photoInfoExchange.uploaderPhotoId)
			assertEquals(-1L, photoInfoExchange.receiverPhotoId)
		}

		kotlin.run {
			val content = webClient
				.post()
				.uri("v1/api/upload")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(BodyInserters.fromMultipartData(multipartData2))
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<UploadPhotoResponse>(content)
			Assert.assertEquals(ErrorCode.UploadPhotoErrors.Ok.value, response.errorCode)

			val photoInfo1 = runBlocking {
				photoInfoDao.findById(1)
			}

			val photoInfo2 = runBlocking {
				photoInfoDao.findById(2)
			}

			assertEquals(1, photoInfo1.exchangeId)
			assertEquals(packet1.userId, photoInfo1.uploaderUserId)
			assertEquals("222", photoInfo1.receiverUserId)
			assertEquals(packet1.isPublic, photoInfo1.isPublic)
			assertEquals(packet1.lon, photoInfo1.lon, EPSILON)
			assertEquals(packet1.lat, photoInfo1.lat, EPSILON)

			assertEquals(1, photoInfo1.exchangeId)
			assertEquals(packet2.userId, photoInfo2.uploaderUserId)
			assertEquals("111", photoInfo2.receiverUserId)
			assertEquals(packet2.isPublic, photoInfo2.isPublic)
			assertEquals(packet2.lon, photoInfo2.lon, EPSILON)
			assertEquals(packet2.lat, photoInfo2.lat, EPSILON)
		}
	}
}