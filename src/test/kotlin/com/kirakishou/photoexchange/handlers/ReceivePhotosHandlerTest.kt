package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.TestUtils.createPhoto
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import net.response.ReceivedPhotosResponse
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
                               photosRepository: PhotosRepository): WebTestClient {
    val handler = ReceivePhotosHandler(jsonConverterService, photosRepository)

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET("/receive_photos/{${Router.PHOTO_NAME_LIST_VARIABLE}}/{${Router.USER_UUID_VARIABLE}}", handler::handle)
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
    val webClient = getWebTestClient(jsonConverterService, photosRepository)

    runBlocking {
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(1, 1L, 6, 1,  "photo1", true, 11.1, 11.1, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(2, 1L, 7, 2,  "photo2", true, 11.1, 11.1, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(3, 1L, 8, 3,  "photo3", true, 11.1, 11.1, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(4, 1L, 9, 4,  "photo4", true, 11.1, 11.1, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(5, 1L, 10, 5, "photo5", true, 11.1, 11.1, 5L, 0L, "123")))

      photosDao.save(PhotoEntity.fromPhoto(createPhoto(6, 2L, 1, 6, "photo6", true, 22.2, 22.2, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(7, 2L, 2, 7, "photo7", true, 22.2, 22.2, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(8, 2L, 3, 8, "photo8", true, 22.2, 22.2, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(9, 2L, 4, 9, "photo9", true, 22.2, 22.2, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(10, 2L, 5, 10, "photo10", true, 22.2, 22.2, 6L, 0L, "123")))
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/receive_photos/photo1,photo2,photo3,photo4,photo5/111")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<ReceivedPhotosResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)
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

      val response = fromBodyContent<ReceivedPhotosResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)
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
    val webClient = getWebTestClient(jsonConverterService, photosRepository)

    runBlocking {
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(1, 1L, 6, -1, "photo1", true, 11.1, 11.1, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(2, 1L, 7, -1, "photo2", true, 11.1, 11.1, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(3, 1L, 8, -1, "photo3", true, 11.1, 11.1, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(4, 1L, 9, -1, "photo4", true, 11.1, 11.1, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(5, 1L, 10, -1, "photo5", true, 11.1, 11.1, 5L, 0L, "123")))

      photosDao.save(PhotoEntity.fromPhoto(createPhoto(6, 2L, 1, -1, "photo6", true, 22.2, 22.2, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(7, 2L, 2, -1, "photo7", true, 22.2, 22.2, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(8, 2L, 3, -1, "photo8", true, 22.2, 22.2, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(9, 2L, 4, -1, "photo9", true, 22.2, 22.2, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(10, 2L, 5, -1, "photo10", true, 22.2, 22.2, 6L, 0L, "123")))
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/receive_photos/photo1,photo2,photo3,photo4,photo5/111")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<ReceivedPhotosResponse>(content)
      assertEquals(ErrorCode.NoPhotosToSendBack.value, response.errorCode)
      assertEquals(0, response.receivedPhotos.size)
    }
  }
}