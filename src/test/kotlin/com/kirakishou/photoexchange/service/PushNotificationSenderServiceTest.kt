package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.TestUtils.createPhoto
import com.kirakishou.photoexchange.core.*
import com.nhaarman.mockitokotlin2.any
import core.SharedConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import reactor.core.publisher.Mono
import kotlin.test.assertTrue

class PushNotificationSenderServiceTest : AbstractTest() {

  private val pushNotificationSenderService = PushNotificationSenderService(
    webClientService,
    usersRepository,
    photosRepository,
    googleCredentialsService,
    jsonConverterService,
    Dispatchers.Unconfined
  )

  private val BAD_FIREBASE_TOKEN = SharedConstants.NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN

  private val photoInfo1 = createPhoto(
    1L,
    ExchangeState.ReadyToExchange,
    1L,
    2L,
    1L,
    "test",
    true,
    11.1,
    22.2,
    66623L,
    0L,
    "4234234"
  )
  private val photoInfo2 = createPhoto(
    2L,
    ExchangeState.ReadyToExchange,
    2L,
    1L,
    2L,
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
        .`when`(usersRepository).getFirebaseToken(any())

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isEmpty())
      Mockito.verify(photosRepository, Mockito.never()).findOneById(any())
    }
  }

  @Test
  fun `should not send push if could not get google token`() {
    dbQuery {
      usersDao.save(UserUuid("111"))
    }

    runBlocking {
      Mockito.doReturn(FirebaseToken("test_token")).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn("").`when`(googleCredentialsService).getAccessToken()

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isNotEmpty())
      Mockito.verify(photosRepository, Mockito.never()).findOneById(any())
    }
  }

  @Test
  fun `should not send push if exchanged photo has already been deleted`() {
    runBlocking {
      Mockito.doReturn(FirebaseToken("test_token")).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn("").`when`(googleCredentialsService).getAccessToken()
      Mockito.doReturn(Photo.empty()).`when`(photosRepository).findOneById(any())

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
      Mockito.doReturn(UserUuid("111")).`when`(usersRepository).getUserUuidByUserId(UserId(1L))
      Mockito.doReturn(FirebaseToken("test_token")).`when`(usersRepository).getFirebaseToken(any())
      Mockito.doReturn("test").`when`(googleCredentialsService).getAccessToken()
      Mockito.doReturn(photoInfo2).`when`(photosRepository).findOneById(any())
      Mockito.doReturn(Mono.just(true)).`when`(webClientService).sendPushNotification(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong())

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isEmpty())
    }
  }
}