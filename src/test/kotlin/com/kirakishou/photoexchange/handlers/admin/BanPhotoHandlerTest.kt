package com.kirakishou.photoexchange.handlers.admin

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.routers.Router
import core.ErrorCode
import kotlinx.coroutines.Dispatchers
import net.response.BanPhotoResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import kotlin.test.assertEquals

class BanPhotoHandlerTest : AbstractTest() {

  private fun getWebTestClient(): WebTestClient {
    val handler = BanPhotoHandler(
      photosRepository,
      adminInfoRepository,
      diskManipulationService,
      Dispatchers.Unconfined,
      jsonConverterService
    )

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET("/ban_photo/{${Router.PHOTO_NAME_VARIABLE}}", handler::handle)
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
  fun `should not do anything if there is no header with admin token`() {
    val webClient = getWebTestClient()

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/ban_photo/test")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()

      val response = fromBodyContent<BanPhotoResponse>(content)
      assertEquals(ErrorCode.BadRequest.value, response.errorCode)
    }
  }

  @Test
  fun `should not do anything if admin token is bad`() {
    val webClient = getWebTestClient()

    Mockito.`when`(adminInfoRepository.adminToken)
      .thenReturn("123")

    kotlin.run {
      val content = webClient
        .get()
        .uri("/v1/api/ban_photo/test")
        .header(ServerSettings.authTokenHeaderName, "456")
        .exchange()
        .expectStatus().isForbidden
        .expectBody()

      val response = fromBodyContent<BanPhotoResponse>(content)
      assertEquals(ErrorCode.BadRequest.value, response.errorCode)
    }
  }
}