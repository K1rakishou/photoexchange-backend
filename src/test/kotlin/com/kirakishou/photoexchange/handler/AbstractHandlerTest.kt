package com.kirakishou.photoexchange.handler

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.LocationMapRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoExchangeRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.model.net.request.SendPhotoPacket
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.service.GeneratorService
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.StaticMapDownloaderService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import com.kirakishou.photoexchange.service.concurrency.TestConcurrencyService
import com.mongodb.ConnectionString
import org.mockito.Mockito
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

abstract class AbstractHandlerTest {
	val EPSILON = 0.00001
	val gson = GsonBuilder().create()

	//any photos should work
	val PHOTO1 = "test_photos/photo_1.jpg"
	val PHOTO2 = "test_photos/photo_2.jpg"
	val PHOTO3 = "test_photos/photo_3.jpg"
	val PHOTO4 = "test_photos/photo_4.jpg"
	val PHOTO5 = "test_photos/photo_5.jpg"
	val PHOTO6 = "test_photos/photo_6.jpg"

	lateinit var template: ReactiveMongoTemplate

	lateinit var concurrentService: AbstractConcurrencyService
	lateinit var jsonConverterService: JsonConverterService

	lateinit var mongoSequenceDao: MongoSequenceDao
	lateinit var photoInfoDao: PhotoInfoDao
	lateinit var photoInfoExchangeDao: PhotoInfoExchangeDao
	lateinit var galleryPhotoDao: GalleryPhotoDao
	lateinit var favouritedPhotoDao: FavouritedPhotoDao
	lateinit var reportedPhotoDao: ReportedPhotoDao
	lateinit var userInfoDao: UserInfoDao
	lateinit var locationMapDao: LocationMapDao

	lateinit var staticMapDownloaderService: StaticMapDownloaderService
	lateinit var locationMapRepository: LocationMapRepository
	lateinit var photoInfoRepository: PhotoInfoRepository
	lateinit var photoInfoExchangeRepository: PhotoInfoExchangeRepository

	fun init() {
		concurrentService = TestConcurrencyService()
		jsonConverterService = JsonConverterService(gson)

		template = ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(
			ConnectionString("mongodb://${ServerSettings.DatabaseInfo.HOST}:${ServerSettings.DatabaseInfo.PORT}/photoexchange_test"))
		)

		mongoSequenceDao = MongoSequenceDao(template).also {
			it.clear()
			it.create()
		}
		photoInfoDao = PhotoInfoDao(template).also {
			it.clear()
			it.create()
		}
		photoInfoExchangeDao = PhotoInfoExchangeDao(template).also {
			it.clear()
			it.create()
		}
		galleryPhotoDao = GalleryPhotoDao(template).also {
			it.clear()
			it.create()
		}
		favouritedPhotoDao = FavouritedPhotoDao(template).also {
			it.clear()
			it.create()
		}
		reportedPhotoDao = ReportedPhotoDao(template).also {
			it.clear()
			it.create()
		}
		userInfoDao = UserInfoDao(template).also {
			it.clear()
			it.create()
		}
		locationMapDao = LocationMapDao(template).also {
			it.clear()
			it.create()
		}

		val generator = GeneratorService()

		photoInfoRepository = PhotoInfoRepository(
			mongoSequenceDao,
			photoInfoDao,
			photoInfoExchangeDao,
			galleryPhotoDao,
			favouritedPhotoDao,
			reportedPhotoDao,
			userInfoDao,
			generator,
			concurrentService
		)

		photoInfoExchangeRepository = PhotoInfoExchangeRepository(
			mongoSequenceDao,
			photoInfoExchangeDao,
			concurrentService
		)

		locationMapRepository = LocationMapRepository(
			mongoSequenceDao,
			locationMapDao,
			concurrentService
		)

		staticMapDownloaderService = Mockito.mock(StaticMapDownloaderService::class.java)
	}

	fun clear() {
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

	inline fun <reified T> fromBodyContent(content: WebTestClient.BodyContentSpec): T {
		return gson.fromJson<UploadPhotoResponse>(String(content.returnResult().responseBody), T::class.java) as T
	}
}