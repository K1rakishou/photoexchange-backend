package com.kirakishou.photoexchange.handlers.admin

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.handler.AbstractHandlerTest
import core.ErrorCode
import net.response.BanUserAndAllTheirPhotosResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router
import java.time.Duration

class BanUserAndAllTheirPhotosHandlerTest : AbstractHandlerTest() {

  private fun getWebTestClient(): WebTestClient {
    val handler = BanUserAndAllTheirPhotosHandler(
      jsonConverterService,
      photoInfoRepository,
      adminInfoRepository,
      banListRepository,
      diskManipulationService
    )

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET("/ban_user_with_photos/{user_id}", handler::handle)
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