package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.core.FileWrapper
import com.kirakishou.photoexchange.database.mongo.entity.LocationMap
import com.nhaarman.mockito_kotlin.any
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StaticMapDownloaderServiceTest : AbstractServiceTest() {

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
    runBlocking {
      Mockito.doReturn(PhotoInfo.empty())
        .`when`(photosRepository).findOneById(Mockito.anyLong())

      assertTrue(staticMapDownloaderService.testEnqueue(1))

      Mockito.verify(locationMapRepository)
        .increaseAttemptsCountAndNextAttemptTime(any(), any())
      Mockito.verify(webClientService, Mockito.never())
        .downloadLocationMap(any(), any(), any())
    }
  }

  @Test
  fun `should not start downloading map when photo is anonymous`() {
    runBlocking {
      val photo = PhotoInfo(1L, 2L, -1L, "111", "222", true, -1.0, -1.0, 999L, 0L, "34234")

      Mockito.doReturn(photo)
        .`when`(photosRepository).findOneById(Mockito.anyLong())

      assertTrue(staticMapDownloaderService.testEnqueue(1))

      Mockito.verify(locationMapRepository)
        .setMapAnonymous(any(), any())
      Mockito.verify(webClientService, Mockito.never())
        .downloadLocationMap(any(), any(), any())
    }
  }

  @Test
  fun `increase failed attempts or set map status to failed when couldn't download map`() {
    runBlocking {
      val photo = PhotoInfo(1L, 2L, -1L, "111", "222", true, 11.0, 22.0, 999L, 0L, "34234")

      Mockito.doReturn(photo)
        .`when`(photosRepository).findOneById(Mockito.anyLong())
      Mockito.doReturn(null)
        .`when`(webClientService).downloadLocationMap(any(), any(), any())

      assertTrue(staticMapDownloaderService.testEnqueue(1))

      Mockito.verify(locationMapRepository)
        .increaseAttemptsCountAndNextAttemptTime(any(), any())
      Mockito.verify(webClientService)
        .downloadLocationMap(any(), any(), any())
    }
  }

  @Test
  fun `should set map status ready when successfully downloaded map`() {
    runBlocking {
      val photo = PhotoInfo(1L, 2L, -1L, "111", "222", true, 11.0, 22.0, 999L, 0L, "34234")

      Mockito.doReturn(photo)
        .`when`(photosRepository).findOneById(Mockito.anyLong())
      Mockito.doReturn(FileWrapper(File("test")))
        .`when`(webClientService).downloadLocationMap(any(), any(), any())

      assertTrue(staticMapDownloaderService.testEnqueue(1))

      Mockito.verify(webClientService)
        .downloadLocationMap(any(), any(), any())
      Mockito.verify(locationMapRepository)
        .setMapReady(any(), any())

      val locationMapList = locationMapDao.testFindAll().awaitFirst()

      assertEquals(1, locationMapList.size)
      assertEquals(LocationMap.MapStatus.Ready.value, locationMapList.first().mapStatus)
    }
  }

  @Test
  fun `should set map status failed when there are no more attempts to download a file`() {
    runBlocking {
      val photo = PhotoInfo(1L, 2L, -1L, "111", "222", true, 11.0, 22.0, 999L, 0L, "34234")

      Mockito.doReturn(photo)
        .`when`(photosRepository).findOneById(Mockito.anyLong())
      Mockito.doReturn(null)
        .`when`(webClientService).downloadLocationMap(any(), any(), any())

      locationMapDao.save(LocationMap(1L, 1L, 6, LocationMap.MapStatus.Empty.value, 10000L)).awaitFirst()

      assertTrue(staticMapDownloaderService.testEnqueue(null))

      Mockito.verify(webClientService)
        .downloadLocationMap(any(), any(), any())
      Mockito.verify(locationMapRepository)
        .setMapFailed(any())
      Mockito.verify(photosRepository, Mockito.times(2))
        .findOneById(any())
      Mockito.verify(diskManipulationService)
        .replaceMapOnDiskWithNoMapAvailablePlaceholder("222")

      val locationMapList = locationMapDao.testFindAll().awaitFirst()

      assertEquals(1, locationMapList.size)
      assertEquals(LocationMap.MapStatus.Failed.value, locationMapList.first().mapStatus)
    }
  }
}