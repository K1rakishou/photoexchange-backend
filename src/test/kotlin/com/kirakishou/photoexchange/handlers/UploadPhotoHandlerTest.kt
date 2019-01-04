package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import com.kirakishou.photoexchange.service.PushNotificationSenderService
import com.kirakishou.photoexchange.service.StaticMapDownloaderService
import com.nhaarman.mockitokotlin2.any
import core.ErrorCode
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.request.UploadPhotoPacket
import net.response.UploadPhotoResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(SpringJUnit4ClassRunner::class)
class UploadPhotoHandlerTest : AbstractTest() {
  private val staticMapDownloaderService = Mockito.mock(StaticMapDownloaderService::class.java)
  private val pushNotificationSenderService = Mockito.mock(PushNotificationSenderService::class.java)

  private fun getWebTestClient(): WebTestClient {
    val handler = UploadPhotoHandler(
      photosRepository,
      usersRepository,
      banListRepository,
      staticMapDownloaderService,
      pushNotificationSenderService,
      remoteAddressExtractorService,
      diskManipulationService,
      cleanupService,
      Dispatchers.Unconfined,
      jsonConverterService
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
      .configureClient().responseTimeout(Duration.ofMinutes(1))
      .build()
  }

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun `photo should not be uploaded if could not enqueue static map downloading request`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(false).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L), UserId(2L)).`when`(usersRepository).getUserIdByUserUuid(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.DatabaseError.value, response.errorCode)

      assertEquals(0, findAllFiles().size)

      dbQuery {
        assertEquals(0, galleryPhotosDao.testFindAll().size)
        assertEquals(0, photosDao.testFindAll().size)
      }
    }
  }

  @Test
  fun `photo should not be uploaded when resizeAndSavePhotos throws an exception`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L), UserId(2L)).`when`(usersRepository).getUserIdByUserUuid(any())

      Mockito.doThrow(IOException("BAM"))
        .`when`(diskManipulationService).resizeAndSavePhotos(any(), any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.ServerResizeError.value, response.errorCode)

      assertEquals(0, findAllFiles().size)

      dbQuery {
        assertEquals(0, galleryPhotosDao.testFindAll().size)
        assertEquals(0, photosDao.testFindAll().size)
      }
    }
  }

  @Test
  fun `photo should not be uploaded when copyDataBuffersToFile throws an exception`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L), UserId(2L)).`when`(usersRepository).getUserIdByUserUuid(any())

      Mockito.doThrow(IOException("BAM"))
        .`when`(diskManipulationService).copyDataBuffersToFile(Mockito.anyList(), any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.ServerDiskError.value, response.errorCode)

      assertEquals(0, findAllFiles().size)

      dbQuery {
        assertEquals(0, galleryPhotosDao.testFindAll().size)
        assertEquals(0, photosDao.testFindAll().size)
      }
    }
  }

  @Test
  fun `should not be allowed to upload photo that exceeds MaxPhotoSize`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L), UserId(2L)).`when`(usersRepository).getUserIdByUserUuid(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(BIG_PHOTO, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.ExceededMaxPhotoSize.value, response.errorCode)
    }
  }

  //TODO: does not work
//  @Test
//  fun `should return RequestPartIsEmpty when packet part is empty`() {
//    val webClient = getWebTestClient()
//
//    runBlocking {
//      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
//      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
//      Mockito.`when`(usersRepository.accountExists(Mockito.anyString())).thenReturn(true)
//    }
//
//    kotlin.run {
//      val multipartData = createMultipartFileWithEmptyPacket(PHOTO1)
//
//      val content = webClient
//        .post()
//        .uri("/v1/api/upload")
//        .contentType(MediaType.MULTIPART_FORM_DATA)
//        .body(BodyInserters.fromMultipartData(multipartData))
//        .exchange()
//        .expectStatus().isBadRequest
//        .expectBody()
//
//      val response = fromBodyContent<UploadPhotoResponse>(content)
//      Assert.assertEquals(ErrorCode.RequestPartIsEmpty.value, response.errorCode)
//    }
//  }

  @Test
  fun `should be allowed to upload photos when firebase token is NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN`() {
    val webClient = getWebTestClient()
    val user1uuid = UserUuid("111")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(FirebaseToken.default()).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L), UserId(2L)).`when`(usersRepository).getUserIdByUserUuid(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)
    }
  }

  @Test
  fun `should not be allowed to upload photos when firebase token is empty`() {
    val webClient = getWebTestClient()
    val user1uuid = UserUuid("111")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(FirebaseToken.empty()).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L), UserId(2L)).`when`(usersRepository).getUserIdByUserUuid(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.UserDoesNotHaveFirebaseToken.value, response.errorCode)
    }
  }

  @Test
  fun `should not be allowed to upload photos when user has no account`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId.empty()).`when`(usersRepository).getUserIdByUserUuid(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.AccountNotFound.value, response.errorCode)
    }
  }

  @Test
  fun `should not be allowed to upload photo when banned`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().isForbidden
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.YouAreBanned.value, response.errorCode)
    }
  }

  @Test
  fun `should not exchange photos when one of the photos has no static map`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")
    val user2uuid = UserUuid("222")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
        usersDao.save(user2uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L), UserId(2L)).`when`(usersRepository).getUserIdByUserUuid(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = dbQuery {
        photosDao.findById(PhotoId(1))
      }

      assertEquals(1, photoInfo.photoId.id)
      assertTrue(photoInfo.exchangedPhotoId.isEmpty())
      assertTrue(photoInfo.locationMapId.isEmpty())
      assertEquals(packet.isPublic, photoInfo.isPublic)
      assertEquals(packet.lon, photoInfo.lon, EPSILON)
      assertEquals(packet.lat, photoInfo.lat, EPSILON)

      val files = findAllFiles()
      assertEquals(4, files.size)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(11.4, 44.2, user2uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO2, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = dbQuery {
        photosDao.findById(PhotoId(2))
      }

      assertEquals(2, photoInfo.photoId.id)
      assertTrue(photoInfo.exchangedPhotoId.isEmpty())
      assertTrue(photoInfo.locationMapId.isEmpty())
      assertEquals(packet.isPublic, photoInfo.isPublic)
      assertEquals(packet.lon, photoInfo.lon, EPSILON)
      assertEquals(packet.lat, photoInfo.lat, EPSILON)

      val files = findAllFiles()
      assertEquals(8, files.size)
    }
  }

  @Test
  fun `test should exchange two photos`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")
    val user2uuid = UserUuid("222")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
        usersDao.save(user2uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L), UserId(2L)).`when`(usersRepository).getUserIdByUserUuid(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = dbQuery {
        photosDao.updateSetLocationMapId(PhotoId(1), LocationMapId(1))
        photosDao.findById(PhotoId(1))
      }

      assertEquals(1, photoInfo.photoId.id)
      assertEquals(1, photoInfo.locationMapId.id)
      assertTrue(photoInfo.exchangedPhotoId.isEmpty())
      assertEquals(packet.isPublic, photoInfo.isPublic)
      assertEquals(packet.lon, photoInfo.lon, EPSILON)
      assertEquals(packet.lat, photoInfo.lat, EPSILON)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(11.4, 24.45, user2uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO2, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo1 = dbQuery {
        photosDao.findById(PhotoId(1))
      }

      val photoInfo2 = dbQuery {
        photosDao.updateSetLocationMapId(PhotoId(2), LocationMapId(2))
        photosDao.findById(PhotoId(2))
      }

      assertEquals(1, photoInfo1.photoId.id)
      assertEquals(2, photoInfo1.exchangedPhotoId.id)
      assertEquals(1, photoInfo1.locationMapId.id)
      assertEquals(1, photoInfo1.userId.id)
      assertEquals(true, photoInfo1.isPublic)
      assertEquals(33.4, photoInfo1.lon, EPSILON)
      assertEquals(55.2, photoInfo1.lat, EPSILON)

      assertEquals(2, photoInfo2.photoId.id)
      assertEquals(1, photoInfo2.exchangedPhotoId.id)
      assertEquals(2, photoInfo2.locationMapId.id)
      assertEquals(2, photoInfo2.userId.id)
      assertEquals(true, photoInfo2.isPublic)
      assertEquals(11.4, photoInfo2.lon, EPSILON)
      assertEquals(24.45, photoInfo2.lat, EPSILON)
    }
  }

  @Test
  fun `test should not exchange two photos with the same user id`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L)).`when`(usersRepository).getUserIdByUserUuid(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = dbQuery {
        photosDao.findById(PhotoId(1))
      }

      assertEquals(1, photoInfo.photoId.id)
      assertTrue(photoInfo.exchangedPhotoId.isEmpty())
      assertEquals(packet.isPublic, photoInfo.isPublic)
      assertEquals(packet.lon, photoInfo.lon, EPSILON)
      assertEquals(packet.lat, photoInfo.lat, EPSILON)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(11.4, 24.45, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO2, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo1 = dbQuery {
        photosDao.findById(PhotoId(1))
      }

      val photoInfo2 = dbQuery {
        photosDao.findById(PhotoId(2))
      }

      assertEquals(1, photoInfo1.photoId.id)
      assertTrue(photoInfo1.exchangedPhotoId.isEmpty())
      assertEquals(true, photoInfo1.isPublic)
      assertEquals(33.4, photoInfo1.lon, EPSILON)
      assertEquals(55.2, photoInfo1.lat, EPSILON)

      assertEquals(2, photoInfo2.photoId.id)
      assertTrue(photoInfo2.exchangedPhotoId.isEmpty())
      assertEquals(true, photoInfo2.isPublic)
      assertEquals(11.4, photoInfo2.lon, EPSILON)
      assertEquals(24.45, photoInfo2.lat, EPSILON)
    }
  }

  @Test
  fun `test should exchange 4 photos`() {
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")
    val user2uuid = UserUuid("222")
    val user3uuid = UserUuid("333")
    val user4uuid = UserUuid("444")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
        usersDao.save(user2uuid)
        usersDao.save(user3uuid)
        usersDao.save(user4uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L), UserId(2L), UserId(3L), UserId(4L)).`when`(usersRepository).getUserIdByUserUuid(any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, user1uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO1, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = dbQuery {
        photosDao.updateSetLocationMapId(PhotoId(1), LocationMapId(1))
        photosDao.findById(PhotoId(1))
      }

      assertEquals(1, photoInfo.photoId.id)
      assertTrue(photoInfo.exchangedPhotoId.isEmpty())
      assertEquals(packet.isPublic, photoInfo.isPublic)
      assertEquals(packet.lon, photoInfo.lon, EPSILON)
      assertEquals(packet.lat, photoInfo.lat, EPSILON)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(11.4, 24.45, user2uuid.uuid, true)
      val multipartData = createTestMultipartFile(PHOTO2, packet)

      val content = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<UploadPhotoResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo1 = dbQuery {
        photosDao.findById(PhotoId(1))
      }

      val photoInfo2 = dbQuery {
        photosDao.updateSetLocationMapId(PhotoId(2), LocationMapId(2))
        photosDao.findById(PhotoId(2))
      }

      assertEquals(1, photoInfo1.photoId.id)
      assertEquals(2, photoInfo1.exchangedPhotoId.id)
      assertEquals(1, photoInfo1.userId.id)
      assertEquals(true, photoInfo1.isPublic)
      assertEquals(33.4, photoInfo1.lon, EPSILON)
      assertEquals(55.2, photoInfo1.lat, EPSILON)

      assertEquals(2, photoInfo2.photoId.id)
      assertEquals(1, photoInfo2.exchangedPhotoId.id)
      assertEquals(2, photoInfo2.userId.id)
      assertEquals(true, photoInfo2.isPublic)
      assertEquals(11.4, photoInfo2.lon, EPSILON)
      assertEquals(24.45, photoInfo2.lat, EPSILON)
    }

    kotlin.run {
      val packet3 = UploadPhotoPacket(36.4, 66.66, user3uuid.uuid, true)
      val multipartData3 = createTestMultipartFile(PHOTO3, packet3)

      val packet4 = UploadPhotoPacket(38.4235, 16.7788, user4uuid.uuid, true)
      val multipartData4 = createTestMultipartFile(PHOTO4, packet4)

      val content1 = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData3))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response1 = fromBodyContent<UploadPhotoResponse>(content1)
      assertEquals(ErrorCode.Ok.value, response1.errorCode)

      dbQuery {
        photosDao.updateSetLocationMapId(PhotoId(3), LocationMapId(3))
      }

      val content2 = webClient
        .post()
        .uri("/v1/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(multipartData4))
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response2 = fromBodyContent<UploadPhotoResponse>(content2)
      assertEquals(ErrorCode.Ok.value, response2.errorCode)

      dbQuery {
        photosDao.updateSetLocationMapId(PhotoId(4), LocationMapId(4))
      }

      val photoInfo1 = dbQuery {
        photosDao.findById(PhotoId(1))
      }

      val photoInfo2 = dbQuery {
        photosDao.findById(PhotoId(2))
      }

      val photoInfo3 = dbQuery {
        photosDao.findById(PhotoId(3))
      }

      val photoInfo4 = dbQuery {
        photosDao.findById(PhotoId(4))
      }

      assertEquals(1, photoInfo1.photoId.id)
      assertEquals(2, photoInfo1.exchangedPhotoId.id)
      assertEquals(1, photoInfo1.userId.id)
      assertEquals(true, photoInfo1.isPublic)
      assertEquals(33.4, photoInfo1.lon, EPSILON)
      assertEquals(55.2, photoInfo1.lat, EPSILON)

      assertEquals(2, photoInfo2.photoId.id)
      assertEquals(1, photoInfo2.exchangedPhotoId.id)
      assertEquals(2, photoInfo2.userId.id)
      assertEquals(true, photoInfo2.isPublic)
      assertEquals(11.4, photoInfo2.lon, EPSILON)
      assertEquals(24.45, photoInfo2.lat, EPSILON)

      assertEquals(3, photoInfo3.photoId.id)
      assertEquals(4, photoInfo3.exchangedPhotoId.id)
      assertEquals(3, photoInfo3.userId.id)
      assertEquals(true, photoInfo3.isPublic)
      assertEquals(36.4, photoInfo3.lon, EPSILON)
      assertEquals(66.66, photoInfo3.lat, EPSILON)

      assertEquals(4, photoInfo4.photoId.id)
      assertEquals(3, photoInfo4.exchangedPhotoId.id)
      assertEquals(4, photoInfo4.userId.id)
      assertEquals(true, photoInfo4.isPublic)
      assertEquals(38.4235, photoInfo4.lon, EPSILON)
      assertEquals(16.7788, photoInfo4.lat, EPSILON)
    }
  }

  @Test
  fun `test 100 concurrent uploadings at the same time`() {
    val concurrency = 100
    val webClient = getWebTestClient()
    val token = FirebaseToken("test_token")
    val user1uuid = UserUuid("111")
    val user2uuid = UserUuid("222")

    runBlocking {
      dbQuery {
        usersDao.save(user1uuid)
        usersDao.save(user2uuid)
      }

      Mockito.doReturn(ipAddress).`when`(remoteAddressExtractorService).extractRemoteAddress(any())
      Mockito.doReturn(true).`when`(staticMapDownloaderService).enqueue(any())
      Mockito.doReturn(false).`when`(banListRepository).isBanned(any())
      Mockito.doReturn(token).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn(UserId(1L)).`when`(usersRepository).getUserIdByUserUuid(user1uuid)
      Mockito.doReturn(UserId(2L)).`when`(usersRepository).getUserIdByUserUuid(user2uuid)
    }

    fun uploadPhoto(packet: UploadPhotoPacket): Mono<Unit> {
      return Mono.fromCallable {
        val multipartData = createTestMultipartFile(PHOTO1, packet)

        val content = webClient
          .post()
          .uri("/v1/api/upload")
          .contentType(MediaType.MULTIPART_FORM_DATA)
          .body(BodyInserters.fromMultipartData(multipartData))
          .exchange()
          .expectStatus().is2xxSuccessful
          .expectBody()

        val response = fromBodyContent<UploadPhotoResponse>(content)
        assertEquals(ErrorCode.Ok.value, response.errorCode)

        dbQuery {
          //set locationMapId for every photo to 10 so they can exchange between themselves
          photosDao.updateSetLocationMapId(PhotoId(response.photoId), LocationMapId(10))
        }

        Unit
      }
    }

    val executor = Executors.newFixedThreadPool(40)

    Flux.range(0, concurrency)
      .flatMap {
        return@flatMap Flux.just(it)
          .subscribeOn(Schedulers.fromExecutor(executor))
          .flatMap { index ->
            println("Sending packet #$index out of $concurrency")

            if (index % 2 == 0) {
              return@flatMap uploadPhoto(UploadPhotoPacket(11.1, 22.2, user1uuid.uuid, true))
            } else {
              return@flatMap uploadPhoto(UploadPhotoPacket(33.3, 44.4, user2uuid.uuid, true))
            }
          }
      }
      .collectList()
      .block()

    val allPhotoInfo = dbQuery {
      photosDao.testFindAll()
    }

    assertEquals(concurrency, allPhotoInfo.size)
    assertEquals(concurrency / 2, allPhotoInfo.count { it.userId.id == 1L })
    assertEquals(concurrency / 2, allPhotoInfo.count { it.userId.id == 2L })

    for (photoInfo in allPhotoInfo) {
      assertNotEquals(photoInfo.photoId.id, photoInfo.exchangedPhotoId.id)
      assertNotEquals(-1L, photoInfo.exchangedPhotoId.id)
    }

    val mapByPhotoId = TreeMap<Long, PhotoEntity>()
    val mapByExchangedPhotoId = TreeMap<Long, PhotoEntity>()

    for (photo in allPhotoInfo) {
      mapByPhotoId[photo.photoId.id] = photo
      mapByExchangedPhotoId[photo.exchangedPhotoId.id] = photo
    }

    assertEquals(mapByPhotoId.size, mapByExchangedPhotoId.size)

    for (photo in mapByPhotoId.values) {
      assertEquals(photo.photoId.id, mapByExchangedPhotoId[photo.exchangedPhotoId.id]!!.photoId.id)
      assertEquals(photo.exchangedPhotoId.id, mapByPhotoId[photo.photoId.id]!!.exchangedPhotoId.id)
    }
  }
}





















