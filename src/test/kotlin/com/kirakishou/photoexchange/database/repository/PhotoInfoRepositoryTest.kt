package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.entity.*
import com.kirakishou.photoexchange.exception.DatabaseTransactionException
import com.nhaarman.mockito_kotlin.any
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class PhotoInfoRepositoryTest : AbstractRepositoryTest() {

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun `deletePhotoInternalInTransaction should cancel deletion when exception occurs inside transaction`() {
    val photoInfo = PhotoInfo(1L, -2L, 0L, "123", "test", true, 11.1, 22.2, 999L, 0L, "12121313")

    runBlocking {
      Mockito
        .doReturn(Mono.error<Boolean>(DatabaseTransactionException("BAM")))
        .`when`(reportedPhotoDao).deleteReportByPhotoNameTransactional(any(), Mockito.anyString())

      val saved = photoInfoDao.save(photoInfo).awaitFirst()
      galleryPhotoDao.save(GalleryPhoto.create(saved.photoId, saved.photoName, 777L)).awaitFirst()
      reportedPhotoDao.reportPhoto(ReportedPhoto(saved.photoId, saved.photoName, saved.userId)).awaitFirst()
      favouritedPhotoDao.favouritePhoto(FavouritedPhoto(saved.photoId, saved.photoName, saved.userId)).awaitFirst()
      locationMapDao.save(LocationMap.create(photoInfo.photoId)).awaitFirst()

      assertFalse(photoInfoRepository.delete(photoInfo))

      assertEquals(photoInfo.photoName, photoInfoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(photoInfo.photoName, favouritedPhotoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(photoInfo.photoName, reportedPhotoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(photoInfo.photoName, galleryPhotoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(photoInfo.photoId, locationMapDao.testFindAll().awaitFirst().first().photoId)
    }
  }

}