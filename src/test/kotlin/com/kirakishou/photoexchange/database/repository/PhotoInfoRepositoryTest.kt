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
import kotlin.test.assertTrue

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
        .`when`(reportedPhotoDao).deleteReportByPhotoName(Mockito.anyString(), any())

      val saved = photoInfoDao.save(photoInfo).awaitFirst()
      galleryPhotoDao.save(GalleryPhoto.create(saved.photoId, saved.photoName, 777L)).awaitFirst()
      reportedPhotoDao.reportPhoto(ReportedPhoto(saved.photoId, saved.photoName, saved.userId)).awaitFirst()
      favouritedPhotoDao.favouritePhoto(FavouritedPhoto(saved.photoId, saved.photoName, saved.userId)).awaitFirst()
      locationMapDao.save(LocationMap.create(photoInfo.photoId)).awaitFirst()

      assertFalse(photoInfoRepository.delete(photoInfo))

      assertEquals(1, photoInfoDao.testFindAll().awaitFirst().size)
      assertEquals(photoInfo.photoName, photoInfoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(photoInfo.photoName, favouritedPhotoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(photoInfo.photoName, reportedPhotoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(photoInfo.photoName, galleryPhotoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(photoInfo.photoId, locationMapDao.testFindAll().awaitFirst().first().photoId)
    }
  }

  @Test
  fun `save method should create gallery photo if uploaded photo is public`() {
    runBlocking {
      val saved = photoInfoRepository.save("4234", 11.1, 22.2, true, 5345L, "23123")

      assertEquals(saved.photoName, photoInfoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(saved.photoName, galleryPhotoDao.testFindAll().awaitFirst().first().photoName)
    }
  }

  @Test
  fun `save method should NOT create gallery photo if uploaded photo is private`() {
    runBlocking {
      val saved = photoInfoRepository.save("4234", 11.1, 22.2, false, 5345L, "23123")

      assertEquals(saved.photoName, photoInfoDao.testFindAll().awaitFirst().first().photoName)
      assertTrue(galleryPhotoDao.testFindAll().awaitFirst().isEmpty())
    }
  }

  @Test
  fun `tryDoExchange should return empty PhotoInfo when there is nothing to exchange with`() {
    runBlocking {
      val userId = "4234"

      val photo = PhotoInfo(1L, -2L, 1L, userId, "ttt", true, 11.1, 22.2, 5345L, 0L, "23123")
      val resultPhoto = photoInfoRepository.tryDoExchange(userId, photo)

      assertTrue(resultPhoto.isEmpty())
    }
  }

  @Test
  fun `tryDoExchange should return old photo when exchange was successful`() {
    runBlocking {
      val userId1 = "111"
      val userId2 = "222"

      val photo1 = PhotoInfo(1L, -2L, 1L, userId1, "ert", true, 11.1, 22.2, 5345L, 0L, "23123")
      val photo2 = PhotoInfo(2L, -2L, 2L, userId2, "ttt", true, 11.1, 22.2, 5345L, 0L, "23123")

      photoInfoDao.save(photo1).awaitFirst()
      photoInfoDao.save(photo2).awaitFirst()

      val resultPhoto = photoInfoRepository.tryDoExchange(userId2, photo2)

      assertEquals(1L, resultPhoto.photoId)
      assertEquals(2L, resultPhoto.exchangedPhotoId)
    }
  }
}