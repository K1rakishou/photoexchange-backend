package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.ReceivePhotosHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.ReceivePhotosResponse
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.JsonConverterService
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import java.time.Duration

@RunWith(SpringJUnit4ClassRunner::class)
class ReceivePhotosHandlerTest : AbstractHandlerTest() {

  private fun getWebTestClient(jsonConverterService: JsonConverterService,
                               photoInfoRepository: PhotoInfoRepository): WebTestClient {
    val handler = ReceivePhotosHandler(jsonConverterService, photoInfoRepository)

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET("/receive_photos/{photo_names}/{user_id}", handler::handle)
          }
        }
      }
    })
      .configureClient().responseTimeout(Duration.ofMillis(1_000_000))
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
  fun `should return uploaded photo name and exchanged photo name list with receiver coordinates`() {
    val webClient = getWebTestClient(jsonConverterService, photoInfoRepository)

    runBlocking {
      photoInfoDao.save(PhotoInfo(1, 6, 1, "111", "photo1", true, 11.1, 11.1, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(2, 7, 2, "111", "photo2", true, 11.1, 11.1, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(3, 8, 3, "111", "photo3", true, 11.1, 11.1, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(4, 9, 4, "111", "photo4", true, 11.1, 11.1, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(5, 10, 5, "111", "photo5", true, 11.1, 11.1, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(6, 1, 6, "222", "photo6", true, 22.2, 22.2, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(7, 2, 7, "222", "photo7", true, 22.2, 22.2, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(8, 3, 8, "222", "photo8", true, 22.2, 22.2, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(9, 4, 9, "222", "photo9", true, 22.2, 22.2, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(10, 5, 10, "222", "photo10", true, 22.2, 22.2, 6L)).awaitFirst()
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/receive_photos/photo1,photo2,photo3,photo4,photo5/111")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<ReceivePhotosResponse>(content)
      assertEquals(ErrorCode.ReceivePhotosErrors.Ok.value, response.errorCode)
      assertEquals(5, response.receivedPhotos.size)

      assertEquals(6, response.receivedPhotos[0].photoId)
      assertEquals("photo1", response.receivedPhotos[0].uploadedPhotoName)
      assertEquals("photo6", response.receivedPhotos[0].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[0].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[0].lat, EPSILON)

      assertEquals(7, response.receivedPhotos[1].photoId)
      assertEquals("photo2", response.receivedPhotos[1].uploadedPhotoName)
      assertEquals("photo7", response.receivedPhotos[1].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[1].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[1].lat, EPSILON)

      assertEquals(8, response.receivedPhotos[2].photoId)
      assertEquals("photo3", response.receivedPhotos[2].uploadedPhotoName)
      assertEquals("photo8", response.receivedPhotos[2].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[2].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[2].lat, EPSILON)

      assertEquals(9, response.receivedPhotos[3].photoId)
      assertEquals("photo4", response.receivedPhotos[3].uploadedPhotoName)
      assertEquals("photo9", response.receivedPhotos[3].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[3].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[3].lat, EPSILON)

      assertEquals(10, response.receivedPhotos[4].photoId)
      assertEquals("photo5", response.receivedPhotos[4].uploadedPhotoName)
      assertEquals("photo10", response.receivedPhotos[4].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[4].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[4].lat, EPSILON)
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/receive_photos/photo6,photo7,photo8,photo9,photo10/222")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<ReceivePhotosResponse>(content)
      assertEquals(ErrorCode.ReceivePhotosErrors.Ok.value, response.errorCode)
      assertEquals(5, response.receivedPhotos.size)

      assertEquals(1, response.receivedPhotos[0].photoId)
      assertEquals("photo6", response.receivedPhotos[0].uploadedPhotoName)
      assertEquals("photo1", response.receivedPhotos[0].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[0].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[0].lat, EPSILON)

      assertEquals(2, response.receivedPhotos[1].photoId)
      assertEquals("photo7", response.receivedPhotos[1].uploadedPhotoName)
      assertEquals("photo2", response.receivedPhotos[1].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[1].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[1].lat, EPSILON)

      assertEquals(3, response.receivedPhotos[2].photoId)
      assertEquals("photo8", response.receivedPhotos[2].uploadedPhotoName)
      assertEquals("photo3", response.receivedPhotos[2].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[2].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[2].lat, EPSILON)

      assertEquals(4, response.receivedPhotos[3].photoId)
      assertEquals("photo9", response.receivedPhotos[3].uploadedPhotoName)
      assertEquals("photo4", response.receivedPhotos[3].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[3].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[3].lat, EPSILON)

      assertEquals(5, response.receivedPhotos[4].photoId)
      assertEquals("photo10", response.receivedPhotos[4].uploadedPhotoName)
      assertEquals("photo5", response.receivedPhotos[4].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[4].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[4].lat, EPSILON)
    }
  }

  @Test
  fun `should not return photo if it does not have location map`() {
    val webClient = getWebTestClient(jsonConverterService, photoInfoRepository)

    runBlocking {
      photoInfoDao.save(PhotoInfo(1, 6, -1, "111", "photo1", true, 11.1, 11.1, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(2, 7, -1, "111", "photo2", true, 11.1, 11.1, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(3, 8, -1, "111", "photo3", true, 11.1, 11.1, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(4, 9, -1, "111", "photo4", true, 11.1, 11.1, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(5, 10, -1, "111", "photo5", true, 11.1, 11.1, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(6, 1, -1, "222", "photo6", true, 22.2, 22.2, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(7, 2, -1, "222", "photo7", true, 22.2, 22.2, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(8, 3, -1, "222", "photo8", true, 22.2, 22.2, 6L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(9, 4, -1, "222", "photo9", true, 22.2, 22.2, 5L)).awaitFirst()
      photoInfoDao.save(PhotoInfo(10, 5, -1, "222", "photo10", true, 22.2, 22.2, 6L)).awaitFirst()
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/receive_photos/photo1,photo2,photo3,photo4,photo5/111")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<ReceivePhotosResponse>(content)
      assertEquals(ErrorCode.ReceivePhotosErrors.NoPhotosToSendBack.value, response.errorCode)
      assertEquals(0, response.receivedPhotos.size)
    }
  }
}