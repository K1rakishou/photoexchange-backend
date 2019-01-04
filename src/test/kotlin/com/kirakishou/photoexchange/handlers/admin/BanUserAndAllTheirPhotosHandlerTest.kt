package com.kirakishou.photoexchange.handlers.admin

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.routers.Router
import core.ErrorCode
import kotlinx.coroutines.Dispatchers
import net.response.BanUserAndAllTheirPhotosResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router

class BanUserAndAllTheirPhotosHandlerTest : AbstractTest() {

  private fun getWebTestClient(): WebTestClient {
    val handler = BanUserAndAllTheirPhotosHandler(
      photosRepository,
      adminInfoRepository,
      banListRepository,
      diskManipulationService,
      Dispatchers.Unconfined,
      jsonConverterService
    )

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET("/ban_user_with_photos/{${Router.USER_UUID_VARIABLE}}", handler::handle)
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
        .uri("/v1/api/ban_user_with_photos/test")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()

      val response = fromBodyContent<BanUserAndAllTheirPhotosResponse>(content)
      kotlin.test.assertEquals(ErrorCode.BadRequest.value, response.errorCode)
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
        .uri("/v1/api/ban_user_with_photos/test")
        .header(ServerSettings.authTokenHeaderName, "456")
        .exchange()
        .expectStatus().isForbidden
        .expectBody()

      val response = fromBodyContent<BanUserAndAllTheirPhotosResponse>(content)
      kotlin.test.assertEquals(ErrorCode.BadRequest.value, response.errorCode)
    }
  }

}