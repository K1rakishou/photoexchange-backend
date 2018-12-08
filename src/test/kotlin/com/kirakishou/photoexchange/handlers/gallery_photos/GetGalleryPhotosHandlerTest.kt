package com.kirakishou.photoexchange.handlers.gallery_photos

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handler.AbstractHandlerTest
import com.kirakishou.photoexchange.database.entity.GalleryPhoto
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.service.JsonConverterService
import core.ErrorCode
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import net.response.GalleryPhotosResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import java.time.Duration

class GetGalleryPhotosHandlerTest : AbstractHandlerTest() {

  private fun getWebTestClient(jsonConverterService: JsonConverterService,
                               photoInfoRepository: PhotoInfoRepository): WebTestClient {
    val handler = GetGalleryPhotosHandler(jsonConverterService, photoInfoRepository)

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET("/get_page_of_gallery_photos/{last_uploaded_on}/{count}", handler::handle)
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
  fun `should return one page of photos sorted by id in descending order`() {
    val webClient = getWebTestClient(jsonConverterService, photoInfoRepository)

    runBlocking {
      photoInfoDao.save(PhotoInfo(1, 1, 3L, "111", "221", true, 11.1, 11.1, 111L, 0L, "123")).awaitFirst()
      photoInfoDao.save(PhotoInfo(2, 2, 4L, "111", "222", true, 11.1, 11.1, 222L, 0L, "123")).awaitFirst()
      photoInfoDao.save(PhotoInfo(3, 3, 2L, "111", "223", true, 11.1, 11.1, 333L, 0L, "123")).awaitFirst()
      photoInfoDao.save(PhotoInfo(4, 4, 1L, "111", "224", true, 11.1, 11.1, 444L, 0L, "123")).awaitFirst()
      photoInfoDao.save(PhotoInfo(5, 5, 9L, "111", "225", true, 11.1, 11.1, 555L, 0L, "123")).awaitFirst()
      photoInfoDao.save(PhotoInfo(6, 6, 8L, "111", "226", true, 11.1, 11.1, 666L, 0L, "123")).awaitFirst()
      photoInfoDao.save(PhotoInfo(7, 7, 7L, "111", "227", true, 11.1, 11.1, 777L, 0L, "123")).awaitFirst()
      photoInfoDao.save(PhotoInfo(8, 8, 6L, "111", "228", true, 11.1, 11.1, 888L, 0L, "123")).awaitFirst()

      galleryPhotoDao.save(GalleryPhoto(1L, 1L, 111L)).awaitFirst()
      galleryPhotoDao.save(GalleryPhoto(2L, 2L, 222L)).awaitFirst()
      galleryPhotoDao.save(GalleryPhoto(3L, 3L, 333L)).awaitFirst()
      galleryPhotoDao.save(GalleryPhoto(4L, 4L, 444L)).awaitFirst()
      galleryPhotoDao.save(GalleryPhoto(5L, 5L, 555L)).awaitFirst()
      galleryPhotoDao.save(GalleryPhoto(6L, 6L, 666L)).awaitFirst()
      galleryPhotoDao.save(GalleryPhoto(7L, 7L, 777L)).awaitFirst()
      galleryPhotoDao.save(GalleryPhoto(8L, 8L, 888L)).awaitFirst()
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

























