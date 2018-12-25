package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.database.entity.PhotoInfo
import core.SharedConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import reactor.core.publisher.Mono
import kotlin.test.assertTrue

class PushNotificationSenderServiceTest : AbstractServiceTest() {

  private val pushNotificationSenderService = PushNotificationSenderService(
    webClientService,
    userInfoRepository,
    photoInfoRepository,
    googleCredentialsService,
    jsonConverterService,
    Dispatchers.Unconfined
  )

  private val BAD_FIREBASE_TOKEN = SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN
  private val GOOD_FIREBASE_TOKEN = "4234234"
  private val BAD_GOOGLE_TOKEN = ""
  private val GOOD_GOOGLE_TOKEN = "tttttttttt"

  private val photoInfo1 = PhotoInfo(
    1L,
    2L,
    1L,
    "111",
    "test",
    true,
    11.1,
    22.2,
    66623L,
    0L,
    "4234234"
  )
  private val photoInfo2 = PhotoInfo(
    2L,
    1L,
    2L,
    "222",
    "test2",
    true,
    22.2,
    33.3,
    54353454L,
    0L,
    "666666"
  )

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun `should not enqueue push request if firebase token is NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN`() {
    runBlocking {
      Mockito.doReturn(BAD_FIREBASE_TOKEN)
        .`when`(userInfoRepository).getFirebaseToken(Mockito.anyString())

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isEmpty())
      Mockito.verify(photoInfoRepository, Mockito.never()).findOneById(Mockito.anyLong())
    }
  }

  @Test
  fun `should not send push if could not get google token`() {
    runBlocking {
      Mockito.doReturn(GOOD_FIREBASE_TOKEN)
        .`when`(userInfoRepository).getFirebaseToken(Mockito.anyString())
      Mockito.doReturn(BAD_GOOGLE_TOKEN)
        .`when`(googleCredentialsService).getAccessToken()

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isNotEmpty())
      Mockito.verify(photoInfoRepository, Mockito.never()).findOneById(Mockito.anyLong())
    }
  }

  @Test
  fun `should not send push if exchanged photo has already been deleted`() {
    runBlocking {
      Mockito.doReturn(GOOD_FIREBASE_TOKEN)
        .`when`(userInfoRepository).getFirebaseToken(Mockito.anyString())
      Mockito.doReturn(GOOD_GOOGLE_TOKEN)
        .`when`(googleCredentialsService).getAccessToken()
      Mockito.doReturn(PhotoInfo.empty())
        .`when`(photoInfoRepository).findOneById(Mockito.anyLong())

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isEmpty())

      Mockito.verify(webClientService, Mockito.never()).sendPushNotification(
        Mockito.anyString(),
        Mockito.anyString(),
        Mockito.anyLong()
      )
    }
  }

  @Test
  fun `should send push`() {
    runBlocking {
      Mockito.doReturn(GOOD_FIREBASE_TOKEN)
        .`when`(userInfoRepository).getFirebaseToken(Mockito.anyString())
      Mockito.doReturn(GOOD_GOOGLE_TOKEN)
        .`when`(googleCredentialsService).getAccessToken()
      Mockito.doReturn(photoInfo2)
        .`when`(photoInfoRepository).findOneById(Mockito.anyLong())
      Mockito.doReturn(Mono.just(true))
        .`when`(webClientService).sendPushNotification(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isEmpty())
    }
  }
}