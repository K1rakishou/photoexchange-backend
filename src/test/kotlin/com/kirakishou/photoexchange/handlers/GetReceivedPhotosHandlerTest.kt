package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.TestUtils.createPhoto
import com.kirakishou.photoexchange.core.ExchangeState
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.Dispatchers
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
class GetReceivedPhotosHandlerTest : AbstractTest() {

  private fun getWebTestClient(jsonConverterService: JsonConverterService,
                               photosRepository: PhotosRepository): WebTestClient {
    val handler = GetReceivedPhotosHandler(
      photosRepository,
      Dispatchers.Unconfined,
      jsonConverterService
    )

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET(
              "/get_page_of_received_photos/{${Router.USER_UUID_VARIABLE}}/{${Router.LAST_UPLOADED_ON_VARIABLE}}/{${Router.COUNT_VARIABLE}}",
              handler::handle
            )
          }
        }
      }
    })
      .configureClient().responseTimeout(Duration.ofMillis(1_000_000))
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
  fun `should return received photos with receiver coordinates`() {
    val webClient = getWebTestClient(jsonConverterService, photosRepository)

    dbQuery {
      assertEquals(1, usersDao.save(UserUuid("111")).userId.id)
      assertEquals(2, usersDao.save(UserUuid("222")).userId.id)

      photosDao.save(PhotoEntity.fromPhoto(createPhoto(1, ExchangeState.Exchanged, 1L, 9, -1L, "photo1", true, 11.1, 11.1, 1L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(2, ExchangeState.Exchanged, 1L, 10, -1L, "photo2", true, 11.1, 11.1, 2L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(3, ExchangeState.Exchanged, 1L, 11, -1L, "photo3", true, 11.1, 11.1, 3L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(4, ExchangeState.Exchanged, 1L, 12, -1L, "photo4", true, 11.1, 11.1, 4L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(5, ExchangeState.Exchanged, 1L, 13, -1L, "photo5", true, 11.1, 11.1, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(6, ExchangeState.Exchanged, 1L, 14, -1L, "photo6", true, 11.1, 11.1, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(7, ExchangeState.Exchanged, 1L, 15, -1L, "photo7", true, 11.1, 11.1, 7L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(8, ExchangeState.Exchanged, 1L, 16, -1L, "photo8", true, 11.1, 11.1, 8L, 0L, "123")))

      photosDao.save(PhotoEntity.fromPhoto(createPhoto(9, ExchangeState.Exchanged, 2L, 1, -1L, "photo9", true, 22.2, 22.2, 1L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(10,ExchangeState.Exchanged,  2L, 2, -1L, "photo10", true, 22.2, 22.2, 2L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(11,ExchangeState.Exchanged,  2L, 3, -1L, "photo11", true, 22.2, 22.2, 3L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(12,ExchangeState.Exchanged,  2L, 4, -1L, "photo12", true, 22.2, 22.2, 4L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(13,ExchangeState.Exchanged,  2L, 5, -1L, "photo13", true, 22.2, 22.2, 5L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(14,ExchangeState.Exchanged,  2L, 6, -1L, "photo14", true, 22.2, 22.2, 6L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(15,ExchangeState.Exchanged,  2L, 7, -1L, "photo15", true, 22.2, 22.2, 7L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(16,ExchangeState.Exchanged,  2L, 8, -1L, "photo16", true, 22.2, 22.2, 8L, 0L, "123")))
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/get_page_of_received_photos/111/8/6")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<ReceivedPhotosResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)
      assertEquals(6, response.receivedPhotos.size)

      assertEquals(15, response.receivedPhotos[0].photoId)
      assertEquals("photo7", response.receivedPhotos[0].uploadedPhotoName)
      assertEquals("photo15", response.receivedPhotos[0].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[0].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[0].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[0].uploadedPhotoName == response.receivedPhotos[0].receivedPhotoName)

      assertEquals(14, response.receivedPhotos[1].photoId)
      assertEquals("photo6", response.receivedPhotos[1].uploadedPhotoName)
      assertEquals("photo14", response.receivedPhotos[1].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[1].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[1].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[1].uploadedPhotoName == response.receivedPhotos[1].receivedPhotoName)

      assertEquals(13, response.receivedPhotos[2].photoId)
      assertEquals("photo5", response.receivedPhotos[2].uploadedPhotoName)
      assertEquals("photo13", response.receivedPhotos[2].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[2].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[2].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[2].uploadedPhotoName == response.receivedPhotos[2].receivedPhotoName)

      assertEquals(12, response.receivedPhotos[3].photoId)
      assertEquals("photo4", response.receivedPhotos[3].uploadedPhotoName)
      assertEquals("photo12", response.receivedPhotos[3].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[3].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[3].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[3].uploadedPhotoName == response.receivedPhotos[3].receivedPhotoName)

      assertEquals(11, response.receivedPhotos[4].photoId)
      assertEquals("photo3", response.receivedPhotos[4].uploadedPhotoName)
      assertEquals("photo11", response.receivedPhotos[4].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[4].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[4].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[4].uploadedPhotoName == response.receivedPhotos[4].receivedPhotoName)

      assertEquals(10, response.receivedPhotos[5].photoId)
      assertEquals("photo2", response.receivedPhotos[5].uploadedPhotoName)
      assertEquals("photo10", response.receivedPhotos[5].receivedPhotoName)
      assertEquals(22.2, response.receivedPhotos[5].lon, EPSILON)
      assertEquals(22.2, response.receivedPhotos[5].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[5].uploadedPhotoName == response.receivedPhotos[5].receivedPhotoName)
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/get_page_of_received_photos/222/8/6")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<ReceivedPhotosResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)
      assertEquals(6, response.receivedPhotos.size)

      assertEquals(7, response.receivedPhotos[0].photoId)
      assertEquals("photo15", response.receivedPhotos[0].uploadedPhotoName)
      assertEquals("photo7", response.receivedPhotos[0].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[0].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[0].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[0].uploadedPhotoName == response.receivedPhotos[0].receivedPhotoName)

      assertEquals(6, response.receivedPhotos[1].photoId)
      assertEquals("photo14", response.receivedPhotos[1].uploadedPhotoName)
      assertEquals("photo6", response.receivedPhotos[1].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[1].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[1].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[1].uploadedPhotoName == response.receivedPhotos[1].receivedPhotoName)

      assertEquals(5, response.receivedPhotos[2].photoId)
      assertEquals("photo13", response.receivedPhotos[2].uploadedPhotoName)
      assertEquals("photo5", response.receivedPhotos[2].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[2].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[2].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[2].uploadedPhotoName == response.receivedPhotos[2].receivedPhotoName)

      assertEquals(4, response.receivedPhotos[3].photoId)
      assertEquals("photo12", response.receivedPhotos[3].uploadedPhotoName)
      assertEquals("photo4", response.receivedPhotos[3].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[3].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[3].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[3].uploadedPhotoName == response.receivedPhotos[3].receivedPhotoName)

      assertEquals(3, response.receivedPhotos[4].photoId)
      assertEquals("photo11", response.receivedPhotos[4].uploadedPhotoName)
      assertEquals("photo3", response.receivedPhotos[4].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[4].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[4].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[4].uploadedPhotoName == response.receivedPhotos[4].receivedPhotoName)

      assertEquals(2, response.receivedPhotos[5].photoId)
      assertEquals("photo10", response.receivedPhotos[5].uploadedPhotoName)
      assertEquals("photo2", response.receivedPhotos[5].receivedPhotoName)
      assertEquals(11.1, response.receivedPhotos[5].lon, EPSILON)
      assertEquals(11.1, response.receivedPhotos[5].lat, EPSILON)
      assertEquals(false, response.receivedPhotos[5].uploadedPhotoName == response.receivedPhotos[5].receivedPhotoName)
    }
  }
}