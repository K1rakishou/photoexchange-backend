package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.TestUtils.createPhoto
import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.entity.LocationMapEntity
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import com.nhaarman.mockitokotlin2.any
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticMapDownloaderServiceTest : AbstractTest() {

  private val staticMapDownloaderService by lazy {
    StaticMapDownloaderService(
      webClientService,
      photosRepository,
      locationMapRepository,
      diskManipulationService,
      Dispatchers.Unconfined
    )
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
  fun `should not start downloading map when there is no photo info associated with this map id`() {
    dbQuery {
      usersDao.save(UserUuid("111"))

      photosDao.save(
        PhotoEntity(
          PhotoId(1),
          ExchangeState.Exchanged,
          UserId(1),
          ExchangedPhotoId.empty(),
          LocationMapId.empty(),
          PhotoName("34234"),
          true,
          11.1,
          22.2,
          444L,
          0L,
          IpHash("1213")
        )
      )
    }

    runBlocking {
      Mockito.doReturn(Photo.empty()).`when`(photosRepository).findOneById(any())

      assertTrue(staticMapDownloaderService.testEnqueue(PhotoId(1)))

      Mockito.verify(locationMapRepository).increaseAttemptsCountAndNextAttemptTime(any(), any())
      Mockito.verify(webClientService, Mockito.never()).downloadLocationMap(any(), any(), any())
    }
  }

  @Test
  fun `should not start downloading map when photo is anonymous`() {
    val photo = createPhoto(
      1L,
      ExchangeState.ReadyToExchange,
      1L,
      2L,
      -1L,
      "222",
      true,
      -1.0,
      -1.0,
      999L,
      0L,
      "34234"
    )

    dbQuery {
      usersDao.save(UserUuid("111"))
      photosDao.save(PhotoEntity.fromPhoto(photo))
    }

    runBlocking {
      Mockito.doReturn(photo).`when`(photosRepository).findOneById(any())

      assertTrue(staticMapDownloaderService.testEnqueue(PhotoId(1)))

      Mockito.verify(locationMapRepository).setMapAnonymous(any(), any())
      Mockito.verify(webClientService, Mockito.never()).downloadLocationMap(any(), any(), any())
    }
  }

  @Test
  fun `increase failed attempts or set map status to failed when couldn't download map`() {
    val photo = createPhoto(
      1L,
      ExchangeState.ReadyToExchange,
      1L,
      2L,
      -1L,
      "222",
      true,
      11.0,
      22.0,
      999L,
      0L,
      "34234"
    )

    dbQuery {
      usersDao.save(UserUuid("111"))
      photosDao.save(PhotoEntity.fromPhoto(photo))
    }

    runBlocking {
      Mockito.doReturn(photo).`when`(photosRepository).findOneById(any())
      Mockito.doReturn(null).`when`(webClientService).downloadLocationMap(any(), any(), any())

      assertTrue(staticMapDownloaderService.testEnqueue(PhotoId(1)))

      Mockito.verify(locationMapRepository).increaseAttemptsCountAndNextAttemptTime(any(), any())
      Mockito.verify(webClientService).downloadLocationMap(any(), any(), any())
    }
  }

  @Test
  fun `should set map status ready when successfully downloaded map`() {
    val photo = createPhoto(
      1L,
      ExchangeState.ReadyToExchange,
      1L,
      2L,
      -1L,
      "222",
      true,
      11.0,
      22.0,
      999L,
      0L,
      "34234"
    )

    dbQuery {
      usersDao.save(UserUuid("111"))
      photosDao.save(PhotoEntity.fromPhoto(photo))
    }

    runBlocking {
      Mockito.doReturn(photo).`when`(photosRepository).findOneById(any())
      Mockito.doReturn(FileWrapper(File("test"))).`when`(webClientService).downloadLocationMap(any(), any(), any())

      assertTrue(staticMapDownloaderService.testEnqueue(PhotoId(1)))

      Mockito.verify(webClientService).downloadLocationMap(any(), any(), any())
      Mockito.verify(locationMapRepository).setMapReady(any(), any())

      val locationMapList = dbQuery { locationMapsDao.testFindAll() }

      assertEquals(1, locationMapList.size)
      assertEquals(LocationMapEntity.MapStatus.Ready, locationMapList.first().mapStatus)
    }
  }

  @Test
  fun `should set map status failed when there are no more attempts to download a file`() {
    val photo = createPhoto(
      1L,
      ExchangeState.ReadyToExchange,
      1L,
      2L,
      -1L,
      "222",
      true,
      11.0,
      22.0,
      999L,
      0L,
      "34234"
    )

    dbQuery {
      usersDao.save(UserUuid("111"))
      photosDao.save(PhotoEntity.fromPhoto(photo))

      locationMapsDao.save(photo.photoId)
      locationMapsDao.incrementAttemptsCount(photo.photoId)
      locationMapsDao.incrementAttemptsCount(photo.photoId)
      locationMapsDao.incrementAttemptsCount(photo.photoId)
      locationMapsDao.incrementAttemptsCount(photo.photoId)
      locationMapsDao.incrementAttemptsCount(photo.photoId)
      locationMapsDao.incrementAttemptsCount(photo.photoId)
    }

    runBlocking {
      Mockito.doReturn(photo).`when`(photosRepository).findOneById(any())
      Mockito.doReturn(FileWrapper(null)).`when`(webClientService).downloadLocationMap(any(), any(), any())

      assertTrue(staticMapDownloaderService.testEnqueue(null))

      Mockito.verify(webClientService).downloadLocationMap(any(), any(), any())
      Mockito.verify(locationMapRepository).setMapFailed(any(), any())
      Mockito.verify(photosRepository, Mockito.times(2)).findOneById(any())
      Mockito.verify(diskManipulationService).replaceMapOnDiskWithNoMapAvailablePlaceholder(PhotoName("222"))

      val locationMapList = dbQuery { locationMapsDao.testFindAll() }

      assertEquals(1, locationMapList.size)
      assertEquals(LocationMapEntity.MapStatus.Failed, locationMapList.first().mapStatus)
    }
  }
}