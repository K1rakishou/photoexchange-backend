package com.kirakishou.photoexchange.service

import com.google.gson.Gson
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.mongodb.ConnectionString
import core.SharedConstants
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import reactor.core.publisher.Mono
import kotlin.test.assertTrue

class PushNotificationSenderServiceTest {

  val template = ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(
    ConnectionString("mongodb://${ServerSettings.TestDatabaseInfo.HOST}:" +
      "${ServerSettings.TestDatabaseInfo.PORT}/${ServerSettings.TestDatabaseInfo.DB_NAME}"))
  )

  lateinit var photoInfoDao: PhotoInfoDao
  lateinit var mongoSequenceDao: MongoSequenceDao
  lateinit var galleryPhotoDao: GalleryPhotoDao
  lateinit var favouritedPhotoDao: FavouritedPhotoDao
  lateinit var reportedPhotoDao: ReportedPhotoDao
  lateinit var locationMapDao: LocationMapDao
  lateinit var userInfoDao: UserInfoDao

  lateinit var generator: GeneratorService
  lateinit var diskManipulationService: DiskManipulationService
  lateinit var jsonConverterService: JsonConverterService
  lateinit var webClientService: WebClientService
  lateinit var googleCredentialsService: GoogleCredentialsService

  lateinit var userInfoRepository: UserInfoRepository
  lateinit var photoInfoRepository: PhotoInfoRepository

  lateinit var pushNotificationSenderService: PushNotificationSenderService

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
  fun setUp() {
    photoInfoDao = Mockito.spy(PhotoInfoDao(template).apply {
      clear()
      create()
    })
    mongoSequenceDao = Mockito.spy(MongoSequenceDao(template).apply {
      clear()
      create()
    })
    galleryPhotoDao = Mockito.spy(GalleryPhotoDao(template).apply {
      clear()
      create()
    })
    favouritedPhotoDao = Mockito.spy(FavouritedPhotoDao(template).apply {
      clear()
      create()
    })
    reportedPhotoDao = Mockito.spy(ReportedPhotoDao(template).apply {
      clear()
      create()
    })
    locationMapDao = Mockito.spy(LocationMapDao(template).apply {
      clear()
      create()
    })
    userInfoDao = Mockito.spy(UserInfoDao(template).apply {
      clear()
      create()
    })

    generator = Mockito.spy(GeneratorService())
    diskManipulationService = Mockito.spy(DiskManipulationService())
    webClientService = Mockito.mock(WebClientService::class.java)
    googleCredentialsService = Mockito.mock(GoogleCredentialsService::class.java)
    jsonConverterService = Mockito.spy(
      JsonConverterService(Gson().newBuilder().create())
    )

    userInfoRepository = Mockito.spy(
      UserInfoRepository(
        mongoSequenceDao,
        userInfoDao,
        generator
      )
    )

    photoInfoRepository = Mockito.spy(
      PhotoInfoRepository(
        template,
        mongoSequenceDao,
        photoInfoDao,
        galleryPhotoDao,
        favouritedPhotoDao,
        reportedPhotoDao,
        locationMapDao,
        generator,
        diskManipulationService
      )
    )

    pushNotificationSenderService = PushNotificationSenderService(
      webClientService,
      userInfoRepository,
      photoInfoRepository,
      googleCredentialsService,
      jsonConverterService
    )
  }

  @After
  fun tearDown() {
    photoInfoDao.clear()
    mongoSequenceDao.clear()
    galleryPhotoDao.clear()
    favouritedPhotoDao.clear()
    reportedPhotoDao.clear()
    locationMapDao.clear()
    userInfoDao.clear()
  }

  @Test
  fun `should not enqueue push request if firebase token is NO_GOOGLE_PLAY_SERVICES_DEFAULT_TOKEN`() {
    runBlocking {
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString()))
        .thenReturn(BAD_FIREBASE_TOKEN)

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isEmpty())
      Mockito.verify(photoInfoRepository, Mockito.never()).findOneById(Mockito.anyLong())
    }
  }

  @Test
  fun `should not send push if could not get google token`() {
    runBlocking {
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString()))
        .thenReturn(GOOD_FIREBASE_TOKEN)
      Mockito.`when`(googleCredentialsService.getAccessToken())
        .thenReturn(BAD_GOOGLE_TOKEN)

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isEmpty())
      Mockito.verify(photoInfoRepository, Mockito.never()).findOneById(Mockito.anyLong())
    }
  }

  @Test
  fun `should not send push if exchanged photo has already been deleted`() {
    runBlocking {
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString()))
        .thenReturn(GOOD_FIREBASE_TOKEN)
      Mockito.`when`(googleCredentialsService.getAccessToken())
        .thenReturn(GOOD_GOOGLE_TOKEN)
      Mockito.`when`(photoInfoRepository.findOneById(Mockito.anyLong()))
        .thenReturn(PhotoInfo.empty())

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
      Mockito.`when`(userInfoRepository.getFirebaseToken(Mockito.anyString()))
        .thenReturn(GOOD_FIREBASE_TOKEN)
      Mockito.`when`(googleCredentialsService.getAccessToken())
        .thenReturn(GOOD_GOOGLE_TOKEN)
      Mockito.`when`(photoInfoRepository.findOneById(Mockito.anyLong()))
        .thenReturn(photoInfo2)
      Mockito.`when`(webClientService.sendPushNotification(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
        .thenReturn(Mono.just(true))

      pushNotificationSenderService.enqueue(photoInfo1)

      assertTrue(pushNotificationSenderService.testGetRequests().isEmpty())
    }
  }
}