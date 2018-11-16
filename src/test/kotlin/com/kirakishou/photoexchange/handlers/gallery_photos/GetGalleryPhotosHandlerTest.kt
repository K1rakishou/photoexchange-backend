package com.kirakishou.photoexchange.handlers.gallery_photos

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handler.AbstractHandlerTest
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.gallery_photos.GalleryPhotosResponse
import com.kirakishou.photoexchange.model.repo.GalleryPhoto
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.JsonConverterService
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
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
						GET("/get_gallery_photos/{last_uploaded_on}/{count}", handler::handle)
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
			photoInfoDao.save(PhotoInfo(1, 1, 3L, "111", "222", "photo1", true, 11.1, 11.1, 111L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(2, 2, 4L, "111", "222", "photo2", true, 11.1, 11.1, 222L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(3, 3, 2L, "111", "222", "photo3", true, 11.1, 11.1, 333L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(4, 4, 1L, "111", "222", "photo4", true, 11.1, 11.1, 444L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(5, 5, 9L, "111", "222", "photo5", true, 11.1, 11.1, 555L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(6, 6, 8L, "111", "222", "photo6", true, 11.1, 11.1, 666L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(7, 7, 7L, "111", "222", "photo7", true, 11.1, 11.1, 777L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(8, 8, 6L, "111", "222", "photo8", true, 11.1, 11.1, 888L)).awaitFirst()

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
				.uri("/v1/api/get_gallery_photos/777/6")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GalleryPhotosResponse>(content)
			assertEquals(ErrorCode.GalleryPhotosErrors.Ok.value, response.errorCode)
			assertEquals(6, response.galleryPhoto.size)

			assertEquals(7, response.galleryPhoto[0].id)
			assertEquals(6, response.galleryPhoto[1].id)
			assertEquals(5, response.galleryPhoto[2].id)
			assertEquals(4, response.galleryPhoto[3].id)
			assertEquals(3, response.galleryPhoto[4].id)
			assertEquals(2, response.galleryPhoto[5].id)
		}
	}
}

























