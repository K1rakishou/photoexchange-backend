package com.kirakishou.photoexchange.handler

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.*
import com.kirakishou.photoexchange.service.*
import com.mongodb.ConnectionString
import kotlinx.coroutines.Dispatchers
import net.request.UploadPhotoPacket
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
import java.io.File
import java.nio.file.Files

abstract class AbstractHandlerTest {
	val EPSILON = 0.00001
	val gson = GsonBuilder().create()
	val filesDir = "D:\\projects\\data\\photos"

	//any photos should work
	val PHOTO1 = "test_photos/photo_1.jpg"
	val PHOTO2 = "test_photos/photo_2.jpg"
	val PHOTO3 = "test_photos/photo_3.jpg"
	val PHOTO4 = "test_photos/photo_4.jpg"
	val PHOTO5 = "test_photos/photo_5.jpg"
	val PHOTO6 = "test_photos/photo_6.jpg"
	val BIG_PHOTO = "test_photos/big_photo.png"

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
	lateinit var banListDao: BanListDao

	lateinit var staticMapDownloaderService: StaticMapDownloaderService
	lateinit var pushNotificationSenderService: PushNotificationSenderService
	lateinit var remoteAddressExtractorService: RemoteAddressExtractorService
	lateinit var diskManipulationService: DiskManipulationService
	lateinit var cleanupService: CleanupService

	lateinit var locationMapRepository: LocationMapRepository
	lateinit var photoInfoRepository: PhotoInfoRepository
  lateinit var userInfoRepository: UserInfoRepository
	lateinit var banListRepository: BanListRepository
	lateinit var adminInfoRepository: AdminInfoRepository

	fun clearFilesDir() {
		val dir = File(filesDir)

		for (file in dir.listFiles()) {
			if (!file.isDirectory) {
				Files.deleteIfExists(file.toPath())
			}
		}
	}

	fun findAllFiles(): Array<File> {
		return File(filesDir).listFiles()
	}

	fun init() {
		clearFilesDir()

		jsonConverterService = JsonConverterService(gson)

		template = ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(
			ConnectionString("mongodb://${ServerSettings.TestDatabaseInfo.HOST}:" +
				"${ServerSettings.TestDatabaseInfo.PORT}/${ServerSettings.TestDatabaseInfo.DB_NAME}"))
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
		banListDao = BanListDao(template).also {
			it.clear()
			it.create()
		}

		val generator = GeneratorService()

		staticMapDownloaderService = Mockito.mock(StaticMapDownloaderService::class.java)
		pushNotificationSenderService = Mockito.mock(PushNotificationSenderService::class.java)
		remoteAddressExtractorService = Mockito.mock(RemoteAddressExtractorService::class.java)
		cleanupService = Mockito.mock(CleanupService::class.java)
		diskManipulationService = Mockito.spy(DiskManipulationService())

		locationMapRepository = LocationMapRepository(
			template,
			mongoSequenceDao,
			locationMapDao,
			photoInfoDao,
			Dispatchers.Unconfined
		)

		userInfoRepository = Mockito.spy(UserInfoRepository(mongoSequenceDao, userInfoDao, generator, Dispatchers.Unconfined))
		banListRepository = Mockito.spy(BanListRepository(mongoSequenceDao, banListDao, Dispatchers.Unconfined))
		adminInfoRepository = Mockito.spy(AdminInfoRepository::class.java)

		photoInfoRepository = PhotoInfoRepository(
			template,
			mongoSequenceDao,
			photoInfoDao,
			galleryPhotoDao,
			favouritedPhotoDao,
			reportedPhotoDao,
			locationMapDao,
			generator,
			diskManipulationService,
			Dispatchers.Unconfined
		)
	}

	fun clear() {
		mongoSequenceDao.clear()
		photoInfoDao.clear()
		galleryPhotoDao.clear()
		favouritedPhotoDao.clear()
		reportedPhotoDao.clear()
		userInfoDao.clear()
		locationMapDao.clear()
		banListDao.clear()

		clearFilesDir()
	}

	fun createTestMultipartFile(fileResourceName: String, packet: UploadPhotoPacket): MultiValueMap<String, Any> {
		val fileResource = ClassPathResource(fileResourceName)

		val photoPart = HttpEntity(fileResource, HttpHeaders().also { it.contentType = MediaType.IMAGE_JPEG })
		val packetPart = HttpEntity(jsonConverterService.toJson(packet), HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON })

		val parts = LinkedMultiValueMap<String, Any>()
		parts.add("photo", photoPart)
		parts.add("packet", packetPart)

		return parts
	}

	fun createMultipartFileWithEmptyPhoto(packet: UploadPhotoPacket): MultiValueMap<String, Any> {
		val photoPart = HttpEntity("",  HttpHeaders().also { it.contentType = MediaType.IMAGE_JPEG })
		val packetPart = HttpEntity(jsonConverterService.toJson(packet), HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON })

		val parts = LinkedMultiValueMap<String, Any>()
		parts.add("photo", photoPart)
		parts.add("packet", packetPart)

		return parts
	}

	fun createMultipartFileWithEmptyPacket(fileResourceName: String): MultiValueMap<String, Any> {
		val fileResource = ClassPathResource(fileResourceName)

		val photoPart = HttpEntity(fileResource, HttpHeaders().also { it.contentType = MediaType.IMAGE_JPEG })
		val packetPart = HttpEntity("", HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON })

		val parts = LinkedMultiValueMap<String, Any>()
		parts.add("photo", photoPart)
		parts.add("packet", packetPart)

		return parts
	}

	inline fun <reified T> fromBodyContent(content: WebTestClient.BodyContentSpec): T {
		return gson.fromJson<T>(String(content.returnResult().responseBody), T::class.java) as T
	}
}