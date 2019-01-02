package com.kirakishou.photoexchange.handlers.admin

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.handlers.AbstractHandlerTest
import com.kirakishou.photoexchange.routers.Router
import core.ErrorCode
import net.response.BanUserResponse
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.server.router

class BanUserHandlerTest : AbstractHandlerTest() {

  private fun getWebTestClient(): WebTestClient {
    val handler = BanUserHandler(
      jsonConverterService,
      photosRepository,
      adminInfoRepository,
      banListRepository
    )

    return WebTestClient.bindToRouterFunction(router {
      "/v1".nest {
        "/api".nest {
          accept(MediaType.APPLICATION_JSON).nest {
            GET("/ban_user/{${Router.USER_UUID_VARIABLE}}", handler::handle)
          }
        }
      }
    })
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
        .uri("/v1/api/ban_user/test")
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()

      val response = fromBodyContent<BanUserResponse>(content)
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
        .uri("/v1/api/ban_user/test")
        .header(ServerSettings.authTokenHeaderName, "456")
        .exchange()
        .expectStatus().isForbidden
        .expectBody()

      val response = fromBodyContent<BanUserResponse>(content)
      kotlin.test.assertEquals(ErrorCode.BadRequest.value, response.errorCode)
    }
  }

}