package com.kirakishou.photoexchange.handler

import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.uploaded_photos.GetUploadedPhotoIdsHandler
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.uploaded_photos.GetUploadedPhotoIdsResponse
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import java.time.Duration

@RunWith(SpringJUnit4ClassRunner::class)
class GetUploadedPhotoIdsTest : AbstractHandlerTest() {

	private fun getWebTestClient(jsonConverterService: JsonConverterService,
								 photoInfoRepository: PhotoInfoRepository,
								 concurrencyService: AbstractConcurrencyService): WebTestClient {
		val handler = GetUploadedPhotoIdsHandler(jsonConverterService, photoInfoRepository, concurrencyService)

		return WebTestClient.bindToRouterFunction(router {
			"/v1".nest {
				"/api".nest {
					accept(MediaType.APPLICATION_JSON).nest {
						GET("/get_uploaded_photo_ids/{user_id}/{last_id}/{count}", handler::handle)
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
	fun `should return photos with and without location map`() {
		val webClient = getWebTestClient(jsonConverterService, photoInfoRepository, concurrentService)

		runBlocking {
			photoInfoDao.save(PhotoInfo(1, 1, -1L, "111", "222", "photo1", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(2, 2, -1L, "111", "222", "photo2", true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(3, 3, -1L, "111", "222", "photo3", true, 11.1, 11.1, 5L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(4, 4, 1L, "111", "222", "photo4", true, 11.1, 11.1, 6L)).awaitFirst()
			photoInfoDao.save(PhotoInfo(5, 5, -1L, "111", "222", "photo5", true, 11.1, 11.1, 5L)).awaitFirst()
		}

		kotlin.run {
			val content = webClient
				.get()
				.uri("v1/api/get_uploaded_photo_ids/111/10000/5")
				.exchange()
				.expectStatus().is2xxSuccessful
				.expectBody()

			val response = fromBodyContent<GetUploadedPhotoIdsResponse>(content)
			assertEquals(ErrorCode.GetUploadedPhotosErrors.Ok.value, response.errorCode)
			assertEquals(5, response.uploadedPhotoIds.size)
		}
	}
}