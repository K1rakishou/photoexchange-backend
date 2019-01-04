package com.kirakishou.photoexchange

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.*
import com.kirakishou.photoexchange.service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.request.UploadPhotoPacket
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
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

abstract class AbstractTest {
  private val factory = TestDatabaseFactory()

  val database: Database by lazy { factory.db }
  val gson = GsonBuilder().create()

  val photosDao = Mockito.spy(PhotosDao())
  val usersDao = Mockito.spy(UsersDao())
  val galleryPhotosDao = Mockito.spy(GalleryPhotosDao())
  val favouritedPhotosDao = Mockito.spy(FavouritedPhotosDao())
  val reportedPhotosDao = Mockito.spy(ReportedPhotosDao())
  val locationMapsDao = Mockito.spy(LocationMapsDao())
  val bansDao = Mockito.spy(BansDao())

  val generator = Mockito.spy(GeneratorService())
  val webClientService = Mockito.mock(WebClientService::class.java)
  val remoteAddressExtractorService = Mockito.mock(RemoteAddressExtractorService::class.java)
  val cleanupService = Mockito.mock(CleanupService::class.java)

  val googleCredentialsService = Mockito.mock(GoogleCredentialsService::class.java)
  val diskManipulationService = Mockito.spy(DiskManipulationService())
  val jsonConverterService = Mockito.spy(JsonConverterService(gson))

  val locationMapRepository = LocationMapRepository(
    locationMapsDao,
    photosDao,
    database,
    Dispatchers.Unconfined
  )

  val usersRepository = Mockito.spy(
    UsersRepository(
      usersDao,
      generator,
      database,
      Dispatchers.Unconfined
    )
  )

  val banListRepository = Mockito.spy(
    BanListRepository(
      bansDao,
      database,
      Dispatchers.Unconfined
    )
  )

  val adminInfoRepository = Mockito.spy(
    AdminInfoRepository::class.java
  )

  val photosRepository = Mockito.spy(
    PhotosRepository(
      photosDao,
      usersDao,
      galleryPhotosDao,
      favouritedPhotosDao,
      reportedPhotosDao,
      locationMapsDao,
      generator,
      diskManipulationService,
      database,
      Dispatchers.IO
    )
  )

  val EPSILON = 0.00001
  val filesDir = "D:\\projects\\data\\photos"

  //any photos should work
  val PHOTO1 = "test_photos/photo_1.jpg"
  val PHOTO2 = "test_photos/photo_2.jpg"
  val PHOTO3 = "test_photos/photo_3.jpg"
  val PHOTO4 = "test_photos/photo_4.jpg"
  val BIG_PHOTO = "test_photos/big_photo.png"

  val ipAddress = "127.0.0.1"

  open fun setUp() {
    clearFilesDir()
    factory.createTables()
  }

  open fun tearDown() {
    factory.dropTables()
    clearFilesDir()
  }

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

  fun <T> dbQuery(block: suspend () -> T): T {
    return transaction(database) {
      runBlocking {
        try {
          block()
        } catch (error: Throwable) {
          error.printStackTrace()
          throw error
        }
      }
    }
  }
}