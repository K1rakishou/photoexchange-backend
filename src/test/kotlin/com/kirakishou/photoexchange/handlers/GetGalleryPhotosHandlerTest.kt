package com.kirakishou.photoexchange.handlers

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.TestUtils.createPhoto
import com.kirakishou.photoexchange.core.PhotoId
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import net.response.GalleryPhotosResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import java.time.Duration

class GetGalleryPhotosHandlerTest : AbstractTest() {

  private fun getWebTestClient(jsonConverterService: JsonConverterService,
                               photosRepository: PhotosRepository): WebTestClient {
    val handler = GetGalleryPhotosHandler(jsonConverterService, photosRepository)

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET("/get_page_of_gallery_photos/{${Router.LAST_UPLOADED_ON_VARIABLE}}/{${Router.COUNT_VARIABLE}}", handler::handle)
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
  fun `should return one page of photos sorted by id in descending order`() {
    val webClient = getWebTestClient(jsonConverterService, photosRepository)

    runBlocking {
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(1, 1, 1, 3L, "221", true, 11.1, 11.1, 111L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(2, 2, 2, 4L, "222", true, 11.1, 11.1, 222L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(3, 3, 3, 2L, "223", true, 11.1, 11.1, 333L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(4, 4, 4, 1L, "224", true, 11.1, 11.1, 444L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(5, 5, 5, 9L, "225", true, 11.1, 11.1, 555L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(6, 6, 6, 8L, "226", true, 11.1, 11.1, 666L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(7, 7, 7, 7L, "227", true, 11.1, 11.1, 777L, 0L, "123")))
      photosDao.save(PhotoEntity.fromPhoto(createPhoto(8, 8, 8, 6L, "228", true, 11.1, 11.1, 888L, 0L, "123")))

      galleryPhotosDao.save(PhotoId(1L), 111L)
      galleryPhotosDao.save(PhotoId(2L), 222L)
      galleryPhotosDao.save(PhotoId(3L), 333L)
      galleryPhotosDao.save(PhotoId(4L), 444L)
      galleryPhotosDao.save(PhotoId(5L), 555L)
      galleryPhotosDao.save(PhotoId(6L), 666L)
      galleryPhotosDao.save(PhotoId(7L), 777L)
      galleryPhotosDao.save(PhotoId(8L), 888L)
    }

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/get_page_of_gallery_photos/777/6")
        .exchange()
        .expectStatus().is2xxSuccessful
        .expectBody()

      val response = fromBodyContent<GalleryPhotosResponse>(content)
      assertEquals(ErrorCode.Ok.value, response.errorCode)
      assertEquals(6, response.galleryPhotos.size)

      assertEquals("226", response.galleryPhotos[0].photoName)
      assertEquals("225", response.galleryPhotos[1].photoName)
      assertEquals("224", response.galleryPhotos[2].photoName)
      assertEquals("223", response.galleryPhotos[3].photoName)
      assertEquals("222", response.galleryPhotos[4].photoName)
      assertEquals("221", response.galleryPhotos[5].photoName)
    }
  }
}

























