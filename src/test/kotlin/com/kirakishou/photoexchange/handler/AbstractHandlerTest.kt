package com.kirakishou.photoexchange.handler

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.BanListRepository
import com.kirakishou.photoexchange.database.repository.LocationMapRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.service.*
import com.mongodb.ConnectionString
import net.request.SendPhotoPacket
import net.response.UploadPhotoResponse
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

	val ipAddress = "127.0.0.1"

	lateinit var template: ReactiveMongoTemplate

	lateinit var jsonConverterService: JsonConverterService

	lateinit var mongoSequenceDao: MongoSequenceDao
	lateinit var photoInfoDao: PhotoInfoDao
	lateinit var galleryPhotoDao: GalleryPhotoDao
	lateinit var favouritedPhotoDao: FavouritedPhotoDao
	lateinit var reportedPhotoDao: ReportedPhotoDao
	lateinit var userInfoDao: UserInfoDao
	lateinit var locationMapDao: LocationMapDao

	lateinit var staticMapDownloaderService: StaticMapDownloaderService
	lateinit var pushNotificationSenderService: PushNotificationSenderService
	lateinit var remoteAddressExtractorService: RemoteAddressExtractorService
	lateinit var diskManipulationService: DiskManipulationService
	lateinit var cleanupService: CleanupService

	lateinit var locationMapRepository: LocationMapRepository
	lateinit var photoInfoRepository: PhotoInfoRepository
  lateinit var userInfoRepository: UserInfoRepository
	lateinit var banListRepository: BanListRepository

	fun init() {
		jsonConverterService = JsonConverterService(gson)

		template = ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(
			ConnectionString("mongodb://${ServerSettings.TestDatabaseInfo.HOST}:${ServerSettings.TestDatabaseInfo.PORT}/${ServerSettings.TestDatabaseInfo.DB_NAME}"))
		)

		mongoSequenceDao = MongoSequenceDao(template).also {
			it.clear()
			it.create()
		}
		photoInfoDao = PhotoInfoDao(template).also {
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
			template,
			mongoSequenceDao,
			photoInfoDao,
			galleryPhotoDao,
			favouritedPhotoDao,
			reportedPhotoDao,
			userInfoDao,
			locationMapDao,
			generator,
			diskManipulationService
		)

		locationMapRepository = LocationMapRepository(
			mongoSequenceDao,
			locationMapDao
		)

		userInfoRepository = Mockito.mock(UserInfoRepository::class.java)
		banListRepository = Mockito.mock(BanListRepository::class.java)
		staticMapDownloaderService = Mockito.mock(StaticMapDownloaderService::class.java)
    pushNotificationSenderService = Mockito.mock(PushNotificationSenderService::class.java)
		remoteAddressExtractorService = Mockito.mock(RemoteAddressExtractorService::class.java)
		diskManipulationService = Mockito.mock(DiskManipulationService::class.java)
		cleanupService = Mockito.mock(CleanupService::class.java)
	}

	fun clear() {
		mongoSequenceDao.clear()
		photoInfoDao.clear()
		galleryPhotoDao.clear()
		favouritedPhotoDao.clear()
		reportedPhotoDao.clear()
		userInfoDao.clear()
		locationMapDao.clear()
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