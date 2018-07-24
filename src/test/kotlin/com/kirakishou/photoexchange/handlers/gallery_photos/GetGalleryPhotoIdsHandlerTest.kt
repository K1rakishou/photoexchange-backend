package com.kirakishou.photoexchange.handlers.gallery_photos

import com.kirakishou.photoexchange.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.handler.AbstractHandlerTest
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.gallery_photos.GalleryPhotoIdsResponse
import com.kirakishou.photoexchange.model.repo.GalleryPhoto
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import java.time.Duration

class GetGalleryPhotoIdsHandlerTest : AbstractHandlerTest() {

	private fun getWebTestClient(jsonConverterService: JsonConverterService,
								 galleryPhotosRepository: GalleryPhotosRepository,
								 concurrencyService: AbstractConcurrencyService): WebTestClient {
		val handler = GetGalleryPhotoIdsHandler(jsonConverterService, galleryPhotosRepository, concurrencyService)

		return WebTestClient.bindToRouterFunction(router {
			"/v1".nest {
				"/api".nest {
					accept(MediaType.APPLICATION_JSON).nest {
						GET("/get_gallery_photo_ids/{last_id}/{count}", handler::handle)
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
	fun `photos should be sorted by id in descending order`() {
		val webClient = getWebTestClient(jsonConverterService, galleryPhotosRepository, concurrentService)

		runBlocking {
			galleryPhotoDao.save(GalleryPhoto(1, 1, 111L)).awaitFirst()
			galleryPhotoDao.save(GalleryPhoto(2, 2, 222L)).awaitFirst()
			galleryPhotoDao.save(GalleryPhoto(3, 3, 333L)).awaitFirst()
			galleryPhotoDao.save(GalleryPhoto(4, 4, 444L)).awaitFirst()
			galleryPhotoDao.save(GalleryPhoto(5, 5, 555L)).awaitFirst()
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("v1/api/get_gallery_photo_ids/10000/5")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GalleryPhotoIdsResponse>(content)
			assertEquals(ErrorCode.GalleryPhotosErrors.Ok.value, response.errorCode)
			assertEquals(5, response.galleryPhotoIds.size)

			assertEquals(5, response.galleryPhotoIds[0])
			assertEquals(4, response.galleryPhotoIds[1])
			assertEquals(3, response.galleryPhotoIds[2])
			assertEquals(2, response.galleryPhotoIds[3])
			assertEquals(1, response.galleryPhotoIds[4])
		}
	}
}


















