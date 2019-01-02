package com.kirakishou.photoexchange.handlers

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.TestDatabaseFactory
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.*
import com.kirakishou.photoexchange.service.*
import kotlinx.coroutines.Dispatchers
import net.request.UploadPhotoPacket
import org.mockito.Mockito
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import java.io.File
import java.nio.file.Files

abstract class AbstractHandlerTest {
  private val database = TestDatabaseFactory().db

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

  lateinit var photosDao: PhotosDao
  lateinit var usersDao: UsersDao
  lateinit var galleryPhotosDao: GalleryPhotosDao
  lateinit var favouritedPhotosDao: FavouritedPhotosDao
  lateinit var reportedPhotosDao: ReportedPhotosDao
  lateinit var locationMapsDao: LocationMapsDao
  lateinit var bansDao: BansDao

  lateinit var jsonConverterService: JsonConverterService
  lateinit var generator: GeneratorService
  lateinit var staticMapDownloaderService: StaticMapDownloaderService
  lateinit var pushNotificationSenderService: PushNotificationSenderService
  lateinit var remoteAddressExtractorService: RemoteAddressExtractorService
  lateinit var diskManipulationService: DiskManipulationService
  lateinit var cleanupService: CleanupService

  lateinit var locationMapRepository: LocationMapRepository
  lateinit var photosRepository: PhotosRepository
  lateinit var usersRepository: UsersRepository
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

    photosDao = Mockito.spy(PhotosDao())
    usersDao = Mockito.spy(UsersDao())
    galleryPhotosDao = Mockito.spy(GalleryPhotosDao())
    favouritedPhotosDao = Mockito.spy(FavouritedPhotosDao())
    reportedPhotosDao = Mockito.spy(ReportedPhotosDao())
    locationMapsDao = Mockito.spy(LocationMapsDao())
    bansDao = Mockito.spy(BansDao())

    val generator = GeneratorService()

    staticMapDownloaderService = Mockito.mock(StaticMapDownloaderService::class.java)
    pushNotificationSenderService = Mockito.mock(PushNotificationSenderService::class.java)
    remoteAddressExtractorService = Mockito.mock(RemoteAddressExtractorService::class.java)
    cleanupService = Mockito.mock(CleanupService::class.java)
    diskManipulationService = Mockito.spy(DiskManipulationService())

    locationMapRepository = LocationMapRepository(
      locationMapsDao,
      photosDao,
      database,
      Dispatchers.Unconfined
    )

    usersRepository = Mockito.spy(
      UsersRepository(
        usersDao,
        generator,
        database,
        Dispatchers.Unconfined
      )
    )
    banListRepository = Mockito.spy(
      BanListRepository(
        bansDao,
        database,
        Dispatchers.Unconfined
      )
    )
    adminInfoRepository = Mockito.spy(AdminInfoRepository::class.java)

    photosRepository = PhotosRepository(
      photosDao,
      usersDao,
      galleryPhotosDao,
      favouritedPhotosDao,
      reportedPhotosDao,
      locationMapsDao,
      generator,
      diskManipulationService,
      database,
      Dispatchers.Unconfined
    )
  }

  fun clear() {
    clearFilesDir()
  }

  fun createTestMultipartFile(fileResourceName: String, packet: UploadPhotoPacket): MultiValueMap<String, Any> {
    val fileResource = ClassPathResource(fileResourceName)

    val photoPart = HttpEntity(fileResource, HttpHeaders().also { it.contentType = MediaType.IMAGE_JPEG })
    val packetPart = HttpEntity(
      jsonConverterService.toJson(packet),
      HttpHeaders().also { it.contentType = MediaType.APPLICATION_JSON })

    val parts = LinkedMultiValueMap<String, Any>()
    parts.add("photo", photoPart)
    parts.add("packet", packetPart)

    return parts
  }

  inline fun <reified T> fromBodyContent(content: WebTestClient.BodyContentSpec): T {
    return gson.fromJson<T>(String(content.returnResult().responseBody), T::class.java) as T
  }
}