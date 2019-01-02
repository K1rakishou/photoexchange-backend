package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.TestUtils.createPhoto
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import net.response.GetUploadedPhotosResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import kotlin.test.assertNotNull

@RunWith(SpringJUnit4ClassRunner::class)
class GetUploadedPhotosHandlerTest : AbstractTest() {

  private fun getWebTestClient(jsonConverterService: JsonConverterService,
                               photosRepository: PhotosRepository): WebTestClient {
    val handler = GetUploadedPhotosHandler(jsonConverterService, photosRepository)

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET("/get_page_of_uploaded_photos/{${Router.USER_UUID_VARIABLE}}/{${Router.LAST_UPLOADED_ON_VARIABLE}}/{${Router.COUNT_VARIABLE}}", handler::handle)
          }
        }
      }
    })
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
  fun `should return uploaded photos with uploader coordinates`() {
    val webClient = getWebTestClient(jsonConverterService, photosRepository)

    runBlocking {
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(1, 1L, 9,  -1L, "photo1",   true, 11.1, 11.1, 1L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(2, 1L, 10, -1L, "photo2",  true, 11.1, 11.1, 2L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(3, 1L, 11, -1L, "photo3",  true, 11.1, 11.1, 3L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(4, 1L, 12, -1L, "photo4",  true, 11.1, 11.1, 4L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(5, 1L, 13, -1L, "photo5",  true, 11.1, 11.1, 5L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(6, 1L, 14, -1L, "photo6",  true, 11.1, 11.1, 6L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(7, 1L, 15, -1L, "photo7",  true, 11.1, 11.1, 7L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(8, 1L, 16, -1L, "photo8",  true, 11.1, 11.1, 8L, 0L, "123")))

     photosDao.save(PhotoEntity.fromPhoto(createPhoto(9,  2L, 1, -1L, "photo9",   true, 22.2, 22.2, 1L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(10, 2L, 2, -1L, "photo10", true, 22.2, 22.2, 2L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(11, 2L, 3, -1L, "photo11", true, 22.2, 22.2, 3L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(12, 2L, 4, -1L, "photo12", true, 22.2, 22.2, 4L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(13, 2L, 5, -1L, "photo13", true, 22.2, 22.2, 5L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(14, 2L, 6, -1L, "photo14", true, 22.2, 22.2, 6L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(15, 2L, 7, -1L, "photo15", true, 22.2, 22.2, 7L, 0L, "123")))
     photosDao.save(PhotoEntity.fromPhoto(createPhoto(16, 2L, 8, -1L, "photo16", true, 22.2, 22.2, 8L, 0L, "123")))
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/get_page_of_uploaded_photos/111/7/6")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<GetUploadedPhotosResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)
      assertEquals(6, response.uploadedPhotos.size)

      assertEquals(6, response.uploadedPhotos[0].photoId)
      assertEquals("photo6", response.uploadedPhotos[0].photoName)
      assertEquals(11.1, response.uploadedPhotos[0].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[0].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[0].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[0].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[0].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(5, response.uploadedPhotos[1].photoId)
      assertEquals("photo5", response.uploadedPhotos[1].photoName)
      assertEquals(11.1, response.uploadedPhotos[1].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[1].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[1].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[1].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[1].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(4, response.uploadedPhotos[2].photoId)
      assertEquals("photo4", response.uploadedPhotos[2].photoName)
      assertEquals(11.1, response.uploadedPhotos[2].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[2].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[2].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[2].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[2].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(3, response.uploadedPhotos[3].photoId)
      assertEquals("photo3", response.uploadedPhotos[3].photoName)
      assertEquals(11.1, response.uploadedPhotos[3].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[3].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[3].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[3].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[3].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(2, response.uploadedPhotos[4].photoId)
      assertEquals("photo2", response.uploadedPhotos[4].photoName)
      assertEquals(11.1, response.uploadedPhotos[4].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[4].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[4].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[4].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[4].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(1, response.uploadedPhotos[5].photoId)
      assertEquals("photo1", response.uploadedPhotos[5].photoName)
      assertEquals(11.1, response.uploadedPhotos[5].uploaderLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[5].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[5].receiverInfoResponseData)
      assertEquals(22.2, response.uploadedPhotos[5].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[5].receiverInfoResponseData!!.receiverLat, EPSILON)
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/get_page_of_uploaded_photos/222/7/6")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<GetUploadedPhotosResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)
      assertEquals(6, response.uploadedPhotos.size)

      assertEquals(14, response.uploadedPhotos[0].photoId)
      assertEquals("photo14", response.uploadedPhotos[0].photoName)
      assertEquals(22.2, response.uploadedPhotos[0].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[0].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[0].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[0].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[0].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(13, response.uploadedPhotos[1].photoId)
      assertEquals("photo13", response.uploadedPhotos[1].photoName)
      assertEquals(22.2, response.uploadedPhotos[1].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[1].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[1].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[1].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[1].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(12, response.uploadedPhotos[2].photoId)
      assertEquals("photo12", response.uploadedPhotos[2].photoName)
      assertEquals(22.2, response.uploadedPhotos[2].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[2].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[2].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[2].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[2].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(11, response.uploadedPhotos[3].photoId)
      assertEquals("photo11", response.uploadedPhotos[3].photoName)
      assertEquals(22.2, response.uploadedPhotos[3].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[3].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[3].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[3].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[3].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(10, response.uploadedPhotos[4].photoId)
      assertEquals("photo10", response.uploadedPhotos[4].photoName)
      assertEquals(22.2, response.uploadedPhotos[4].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[4].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[4].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[4].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[4].receiverInfoResponseData!!.receiverLat, EPSILON)

      assertEquals(9, response.uploadedPhotos[5].photoId)
      assertEquals("photo9", response.uploadedPhotos[5].photoName)
      assertEquals(22.2, response.uploadedPhotos[5].uploaderLon, EPSILON)
      assertEquals(22.2, response.uploadedPhotos[5].uploaderLat, EPSILON)
      assertNotNull(response.uploadedPhotos[5].receiverInfoResponseData)
      assertEquals(11.1, response.uploadedPhotos[5].receiverInfoResponseData!!.receiverLon, EPSILON)
      assertEquals(11.1, response.uploadedPhotos[5].receiverInfoResponseData!!.receiverLat, EPSILON)
    }
  }
}