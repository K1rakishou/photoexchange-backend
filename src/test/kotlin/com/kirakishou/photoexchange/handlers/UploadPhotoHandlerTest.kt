package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.handler.AbstractHandlerTest
import com.nhaarman.mockito_kotlin.any
import core.ErrorCode
import core.SharedConstants
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import net.request.UploadPhotoPacket
import net.response.UploadPhotoResponse
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.IOException
import java.time.Duration
import java.util.concurrent.Executors

@RunWith(SpringJUnit4ClassRunner::class)
class UploadPhotoHandlerTest : AbstractHandlerTest() {

  private fun getWebTestClient(): WebTestClient {
    val handler = UploadPhotoHandler(
      jsonConverterService,
      photoInfoRepository,
      userInfoRepository,
      banListRepository,
      staticMapDownloaderService,
      pushNotificationSenderService,
      remoteAddressExtractorService,
      diskManipulationService,
      cleanupService
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
      .configureClient()
      .responseTimeout(Duration.ofMillis(1_000_000))
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
  fun `photo should not be uploaded when resizeAndSavePhotos throws an exception`() {
    val webClient = getWebTestClient()
    val userId = "1234235236"
    val token = "fwerwe"

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(userInfoRepository.accountExists(userId)).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn(token)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)

      Mockito.doThrow(IOException("BAM"))
        .`when`(diskManipulationService).resizeAndSavePhotos(any(), any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, userId, true)
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

      runBlocking {
        assertEquals(0, galleryPhotoDao.testFindAll().awaitFirst().size)
        assertEquals(0, photoInfoDao.testFindAll().awaitFirst().size)
      }
    }
  }

  @Test
  fun `photo should not be uploaded when copyDataBuffersToFile throws an exception`() {
    val webClient = getWebTestClient()
    val userId = "1234235236"
    val token = "fwerwe"

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(userInfoRepository.accountExists(userId)).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn(token)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)

      Mockito.doThrow(IOException("BAM"))
        .`when`(diskManipulationService).copyDataBuffersToFile(Mockito.anyList(), any())
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, userId, true)
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

      runBlocking {
        assertEquals(0, galleryPhotoDao.testFindAll().awaitFirst().size)
        assertEquals(0, photoInfoDao.testFindAll().awaitFirst().size)
      }
    }
  }

  @Test
  fun `photo should not be uploaded if could not enqueue static map downloading request`() {
    val webClient = getWebTestClient()
    val userId = "1234235236"
    val token = "fwerwe"

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(userInfoRepository.accountExists(userId)).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn(token)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(false)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, userId, true)
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

      runBlocking {
        assertEquals(0, galleryPhotoDao.testFindAll().awaitFirst().size)
        assertEquals(0, photoInfoDao.testFindAll().awaitFirst().size)
      }
    }
  }

  @Test
  fun `should not be allowed to upload photo that exceeds MaxPhotoSize`() {
    val webClient = getWebTestClient()
    val userId = "1234235236"
    val token = "fwerwe"

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(userInfoRepository.accountExists(userId)).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn(token)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, userId, true)
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
      Assert.assertEquals(ErrorCode.ExceededMaxPhotoSize.value, response.errorCode)
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
//      Mockito.`when`(userInfoRepository.accountExists(Mockito.anyString())).thenReturn(true)
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
    val userId = "1234235236"

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(userInfoRepository.accountExists(userId)).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn(SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, userId, true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)
    }
  }

  @Test
  fun `should not be allowed to upload photos when firebase token is empty`() {
    val webClient = getWebTestClient()
    val token = ""
    val userId = "1234235236"

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(userInfoRepository.accountExists(userId)).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn(token)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, userId, true)
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
      Assert.assertEquals(ErrorCode.UserDoesNotHaveFirebaseToken.value, response.errorCode)
    }
  }

  @Test
  fun `should not be allowed to upload photos when user has no account`() {
    val webClient = getWebTestClient()
    val userId = "1234235236"

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(userInfoRepository.accountExists(userId)).thenReturn(false)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, userId, true)
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
      Assert.assertEquals(ErrorCode.AccountNotFound.value, response.errorCode)
    }
  }

  @Test
  fun `should not be allowed to upload photo when banned`() {
    val webClient = getWebTestClient()

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn("test_token")
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, "111", true)
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
      Assert.assertEquals(ErrorCode.YouAreBanned.value, response.errorCode)
    }
  }

  @Test
  fun `should not exchange photos when one of the photos has no static map`() {
    val webClient = getWebTestClient()

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn("test_token")
      Mockito.`when`(userInfoRepository.accountExists(Mockito.anyString())).thenReturn(true)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, "111", true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = runBlocking {
        photoInfoDao.findById(1).awaitFirst()
      }

      assertEquals(1, photoInfo.photoId)
      assertEquals(PhotoInfo.EMPTY_PHOTO_ID, photoInfo.exchangedPhotoId)
      assertEquals(PhotoInfo.EMPTY_LOCATION_MAP_ID, photoInfo.locationMapId)
      assertEquals(packet.userId, photoInfo.userId)
      assertEquals(packet.isPublic, photoInfo.isPublic)
      assertEquals(packet.lon, photoInfo.lon, EPSILON)
      assertEquals(packet.lat, photoInfo.lat, EPSILON)

      val files = findAllFiles()
      assertEquals(4, files.size)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(11.4, 44.2, "222", true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = runBlocking {
        photoInfoDao.findById(2).awaitFirst()
      }

      assertEquals(2, photoInfo.photoId)
      assertEquals(PhotoInfo.EMPTY_PHOTO_ID, photoInfo.exchangedPhotoId)
      assertEquals(PhotoInfo.EMPTY_LOCATION_MAP_ID, photoInfo.locationMapId)
      assertEquals(packet.userId, photoInfo.userId)
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

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn("test_token")
      Mockito.`when`(userInfoRepository.accountExists(Mockito.anyString())).thenReturn(true)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, "111", true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = runBlocking {
        photoInfoDao.updateSetLocationMapId(1, 1).awaitFirst()
        photoInfoDao.findById(1).awaitFirst()
      }

      assertEquals(1, photoInfo.photoId)
      assertEquals(PhotoInfo.EMPTY_PHOTO_ID, photoInfo.exchangedPhotoId)
      assertEquals(1, photoInfo.locationMapId)
      assertEquals(packet.userId, photoInfo.userId)
      assertEquals(packet.isPublic, photoInfo.isPublic)
      assertEquals(packet.lon, photoInfo.lon, EPSILON)
      assertEquals(packet.lat, photoInfo.lat, EPSILON)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(11.4, 24.45, "222", true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo1 = runBlocking {
        photoInfoDao.findById(1).awaitFirst()
      }

      val photoInfo2 = runBlocking {
        photoInfoDao.updateSetLocationMapId(2, 2).awaitFirst()
        photoInfoDao.findById(2).awaitFirst()
      }

      assertEquals(1, photoInfo1.photoId)
      assertEquals(2, photoInfo1.exchangedPhotoId)
      assertEquals(1, photoInfo1.locationMapId)
      assertEquals("111", photoInfo1.userId)
      assertEquals(true, photoInfo1.isPublic)
      assertEquals(33.4, photoInfo1.lon, EPSILON)
      assertEquals(55.2, photoInfo1.lat, EPSILON)

      assertEquals(2, photoInfo2.photoId)
      assertEquals(1, photoInfo2.exchangedPhotoId)
      assertEquals(2, photoInfo2.locationMapId)
      assertEquals("222", photoInfo2.userId)
      assertEquals(true, photoInfo2.isPublic)
      assertEquals(11.4, photoInfo2.lon, EPSILON)
      assertEquals(24.45, photoInfo2.lat, EPSILON)
    }
  }

  @Test
  fun `test should not exchange two photos with the same user id`() {
    val webClient = getWebTestClient()

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn("test_token")
      Mockito.`when`(userInfoRepository.accountExists(Mockito.anyString())).thenReturn(true)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, "111", true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = runBlocking {
        photoInfoDao.findById(1).awaitFirst()
      }

      assertEquals(1, photoInfo.photoId)
      assertEquals(PhotoInfo.EMPTY_PHOTO_ID, photoInfo.exchangedPhotoId)
      assertEquals(packet.userId, photoInfo.userId)
      assertEquals(packet.isPublic, photoInfo.isPublic)
      assertEquals(packet.lon, photoInfo.lon, EPSILON)
      assertEquals(packet.lat, photoInfo.lat, EPSILON)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(11.4, 24.45, "111", true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo1 = runBlocking {
        photoInfoDao.findById(1).awaitFirst()
      }

      val photoInfo2 = runBlocking {
        photoInfoDao.findById(2).awaitFirst()
      }

      assertEquals(1, photoInfo1.photoId)
      assertEquals(PhotoInfo.EMPTY_PHOTO_ID, photoInfo1.exchangedPhotoId)
      assertEquals(packet.userId, photoInfo1.userId)
      assertEquals(true, photoInfo1.isPublic)
      assertEquals(33.4, photoInfo1.lon, EPSILON)
      assertEquals(55.2, photoInfo1.lat, EPSILON)

      assertEquals(2, photoInfo2.photoId)
      assertEquals(PhotoInfo.EMPTY_PHOTO_ID, photoInfo2.exchangedPhotoId)
      assertEquals(packet.userId, photoInfo2.userId)
      assertEquals(true, photoInfo2.isPublic)
      assertEquals(11.4, photoInfo2.lon, EPSILON)
      assertEquals(24.45, photoInfo2.lat, EPSILON)
    }
  }

  @Test
  fun `test should exchange 4 photos`() {
    val webClient = getWebTestClient()

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn("test_token")
      Mockito.`when`(userInfoRepository.accountExists(Mockito.anyString())).thenReturn(true)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(33.4, 55.2, "111", true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo = runBlocking {
        photoInfoDao.updateSetLocationMapId(1, 1).awaitFirst()
        photoInfoDao.findById(1).awaitFirst()
      }

      assertEquals(1, photoInfo.photoId)
      assertEquals(PhotoInfo.EMPTY_PHOTO_ID, photoInfo.exchangedPhotoId)
      assertEquals(packet.userId, photoInfo.userId)
      assertEquals(packet.isPublic, photoInfo.isPublic)
      assertEquals(packet.lon, photoInfo.lon, EPSILON)
      assertEquals(packet.lat, photoInfo.lat, EPSILON)
    }

    kotlin.run {
      val packet = UploadPhotoPacket(11.4, 24.45, "222", true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)

      val photoInfo1 = runBlocking {
        photoInfoDao.findById(1).awaitFirst()
      }

      val photoInfo2 = runBlocking {
        photoInfoDao.updateSetLocationMapId(2, 2).awaitFirst()
        photoInfoDao.findById(2).awaitFirst()
      }

      assertEquals(1, photoInfo1.photoId)
      assertEquals(2, photoInfo1.exchangedPhotoId)
      assertEquals("111", photoInfo1.userId)
      assertEquals(true, photoInfo1.isPublic)
      assertEquals(33.4, photoInfo1.lon, EPSILON)
      assertEquals(55.2, photoInfo1.lat, EPSILON)

      assertEquals(2, photoInfo2.photoId)
      assertEquals(1, photoInfo2.exchangedPhotoId)
      assertEquals("222", photoInfo2.userId)
      assertEquals(true, photoInfo2.isPublic)
      assertEquals(11.4, photoInfo2.lon, EPSILON)
      assertEquals(24.45, photoInfo2.lat, EPSILON)
    }

    kotlin.run {
      val packet3 = UploadPhotoPacket(36.4, 66.66, "333", true)
      val multipartData3 = createTestMultipartFile(PHOTO3, packet3)

      val packet4 = UploadPhotoPacket(38.4235, 16.7788, "444", true)
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
      Assert.assertEquals(ErrorCode.Ok.value, response1.errorCode)

      runBlocking {
        photoInfoDao.updateSetLocationMapId(3, 3).awaitFirst()
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
      Assert.assertEquals(ErrorCode.Ok.value, response2.errorCode)

      runBlocking {
        photoInfoDao.updateSetLocationMapId(4, 4).awaitFirst()
      }

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

      assertEquals(1, photoInfo1.photoId)
      assertEquals(2, photoInfo1.exchangedPhotoId)
      assertEquals("111", photoInfo1.userId)
      assertEquals(true, photoInfo1.isPublic)
      assertEquals(33.4, photoInfo1.lon, EPSILON)
      assertEquals(55.2, photoInfo1.lat, EPSILON)

      assertEquals(2, photoInfo2.photoId)
      assertEquals(1, photoInfo2.exchangedPhotoId)
      assertEquals("222", photoInfo2.userId)
      assertEquals(true, photoInfo2.isPublic)
      assertEquals(11.4, photoInfo2.lon, EPSILON)
      assertEquals(24.45, photoInfo2.lat, EPSILON)

      assertEquals(3, photoInfo3.photoId)
      assertEquals(4, photoInfo3.exchangedPhotoId)
      assertEquals("333", photoInfo3.userId)
      assertEquals(true, photoInfo3.isPublic)
      assertEquals(36.4, photoInfo3.lon, EPSILON)
      assertEquals(66.66, photoInfo3.lat, EPSILON)

      assertEquals(4, photoInfo4.photoId)
      assertEquals(3, photoInfo4.exchangedPhotoId)
      assertEquals("444", photoInfo4.userId)
      assertEquals(true, photoInfo4.isPublic)
      assertEquals(38.4235, photoInfo4.lon, EPSILON)
      assertEquals(16.7788, photoInfo4.lat, EPSILON)
    }
  }

  @Test
  fun `test 100 concurrent uploadings at the same time`() {
    val concurrency = 100
    val webClient = getWebTestClient()

    runBlocking {
      Mockito.`when`(remoteAddressExtractorService.extractRemoteAddress(any())).thenReturn(ipAddress)
      Mockito.`when`(banListRepository.isBanned(Mockito.anyString())).thenReturn(false)
      Mockito.`when`(staticMapDownloaderService.enqueue(Mockito.anyLong())).thenReturn(true)
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString())).thenReturn("test_token")
      Mockito.`when`(userInfoRepository.accountExists(Mockito.anyString())).thenReturn(true)
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
        Assert.assertEquals(ErrorCode.Ok.value, response.errorCode)

        runBlocking {
          //set locationMapId for every photo to 10 so they can exchange between themselves
          photoInfoDao.updateSetLocationMapId(response.photoId, 10).awaitFirst()
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
              return@flatMap uploadPhoto(UploadPhotoPacket(11.1, 22.2, "111", true))
            } else {
              return@flatMap uploadPhoto(UploadPhotoPacket(33.3, 44.4, "222", true))
            }
          }
      }
      .collectList()
      .block()

    runBlocking {
      val allPhotoInfo = photoInfoDao.testFindAll().awaitFirst()
      assertEquals(concurrency, allPhotoInfo.size)

      for (photoInfo in allPhotoInfo) {
        assertEquals(true, photoInfo.photoId != photoInfo.exchangedPhotoId)
        assertEquals(false, photoInfo.exchangedPhotoId == -1L)
      }

      val mapByPhotoId = allPhotoInfo.associateBy { it.photoId }
      val mapByExchangedPhotoId = allPhotoInfo.associateBy { it.exchangedPhotoId }

      for (photo in mapByPhotoId.values) {
        assertEquals(photo.photoId, mapByExchangedPhotoId[photo.exchangedPhotoId]!!.photoId)
        assertEquals(photo.exchangedPhotoId, mapByPhotoId[photo.photoId]!!.exchangedPhotoId)
      }
    }
  }
}




















