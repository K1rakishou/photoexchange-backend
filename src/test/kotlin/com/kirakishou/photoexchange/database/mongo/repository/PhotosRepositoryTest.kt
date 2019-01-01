package com.kirakishou.photoexchange.database.mongo.repository

import com.kirakishou.photoexchange.Utils
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.config.ServerSettings.FILE_DIR_PATH
import com.kirakishou.photoexchange.database.mongo.entity.FavouritedPhoto
import com.kirakishou.photoexchange.database.mongo.entity.GalleryPhoto
import com.kirakishou.photoexchange.database.mongo.entity.ReportedPhoto
import com.kirakishou.photoexchange.exception.DatabaseTransactionException
import com.nhaarman.mockito_kotlin.any
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Mono
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhotosRepositoryTest : AbstractRepositoryTest() {

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

      assertFalse(photosRepository.delete(photoInfo))

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
      val saved = photosRepository.save("4234", 11.1, 22.2, true, 5345L, "23123")

      assertEquals(saved.photoName, photoInfoDao.testFindAll().awaitFirst().first().photoName)
      assertEquals(saved.photoName, galleryPhotoDao.testFindAll().awaitFirst().first().photoName)
    }
  }

  @Test
  fun `save method should NOT create gallery photo if uploaded photo is private`() {
    runBlocking {
      val saved = photosRepository.save("4234", 11.1, 22.2, false, 5345L, "23123")

      assertEquals(saved.photoName, photoInfoDao.testFindAll().awaitFirst().first().photoName)
      assertTrue(galleryPhotoDao.testFindAll().awaitFirst().isEmpty())
    }
  }

  @Test
  fun `tryDoExchange should return empty PhotoInfo when there is nothing to exchange with`() {
    runBlocking {
      val userId = "4234"

      val photo = PhotoInfo(1L, -2L, 1L, userId, "ttt", true, 11.1, 22.2, 5345L, 0L, "23123")
      val resultPhoto = photosRepository.tryDoExchange(userId, photo)

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

      val resultPhoto = photosRepository.tryDoExchange(userId2, photo2)

      assertEquals(1L, resultPhoto.photoId)
      assertEquals(2L, resultPhoto.exchangedPhotoId)
    }
  }

  @Test
  fun `should mark as deleted 4 photos`() {
    runBlocking {
      val generatedPhotos = Utils.createExchangedPhotoPairs(10, listOf("111", "222"))

      for (generatedPhoto in generatedPhotos) {
        photoInfoDao.save(generatedPhoto).awaitFirst()
      }

      val count = photosRepository.markAsDeletedPhotosUploadedEarlierThan(
        104L,
        999L,
        100
      )

      assertEquals(4, count)

      val found = photoInfoDao.testFindAll().awaitFirst()
      assertEquals(4, found.count { it.deletedOn == 999L })
    }
  }

  @Test
  fun `should delete all photos with deletedOn field greater than zero`() {
    runBlocking {
      val generatedPhotos = Utils.createExchangedPhotoPairs(10, listOf("111", "222"))
        .map { it.copy(deletedOn = 111L) }

      for (generatedPhoto in generatedPhotos) {
        photoInfoDao.save(generatedPhoto).awaitFirst()

        val path = "files\\no_map_available\\no_map_available.png"
        val file = ClassPathResource(path).file

        file.copyTo(File("${FILE_DIR_PATH}\\${generatedPhoto.photoName}${ServerSettings.VERY_BIG_PHOTO_SUFFIX}"))
        file.copyTo(File("${FILE_DIR_PATH}\\${generatedPhoto.photoName}${ServerSettings.BIG_PHOTO_SUFFIX}"))
        file.copyTo(File("${FILE_DIR_PATH}\\${generatedPhoto.photoName}${ServerSettings.MEDIUM_PHOTO_SUFFIX}"))
        file.copyTo(File("${FILE_DIR_PATH}\\${generatedPhoto.photoName}${ServerSettings.SMALL_PHOTO_SUFFIX}"))
        file.copyTo(File("${FILE_DIR_PATH}\\${generatedPhoto.photoName}${ServerSettings.PHOTO_MAP_SUFFIX}"))
      }

      photosRepository.cleanDatabaseAndPhotos(200L, 100)

      assertTrue(File(FILE_DIR_PATH).list().isEmpty())

      //FIXME: this assert does not work now because transactions do not work (but should work once transactions are fixed)
      assertTrue(photoInfoDao.testFindAll().awaitFirst().isEmpty())
    }
  }

  @Test
  fun `should favourite and then unfavourite photos`() {
    runBlocking {
      val generatedPhotos = Utils.createExchangedPhotoPairs(2, listOf("111", "222"))

      for (generatedPhoto in generatedPhotos) {
        photoInfoDao.save(generatedPhoto).awaitFirst()
      }

      for (generatedPhoto in generatedPhotos) {
        photosRepository.favouritePhoto("333", generatedPhoto.photoName)
        photosRepository.favouritePhoto("444", generatedPhoto.photoName)
        photosRepository.favouritePhoto("555", generatedPhoto.photoName)
      }

      val favouritedPhotos = favouritedPhotoDao.testFindAll().awaitFirst()
      assertEquals(6, favouritedPhotos.size)

      val groupedByNamePhotos = favouritedPhotos.groupBy { it.photoName }
      val groupedByUserIdPhotos = favouritedPhotos.groupBy { it.userId }

      for (generatedPhoto in generatedPhotos) {
        assertEquals(3, groupedByNamePhotos[generatedPhoto.photoName]!!.size)
      }

      assertEquals(2, groupedByUserIdPhotos["333"]!!.size)
      assertEquals(2, groupedByUserIdPhotos["444"]!!.size)
      assertEquals(2, groupedByUserIdPhotos["555"]!!.size)

      for (generatedPhoto in generatedPhotos) {
        photosRepository.favouritePhoto("333", generatedPhoto.photoName)
        photosRepository.favouritePhoto("444", generatedPhoto.photoName)
        photosRepository.favouritePhoto("555", generatedPhoto.photoName)
      }

      assertTrue(favouritedPhotoDao.testFindAll().awaitFirst().isEmpty())
    }
  }

  @Test
  fun `should report and then unreport photos`() {
    runBlocking {
      val generatedPhotos = Utils.createExchangedPhotoPairs(2, listOf("111", "222"))

      for (generatedPhoto in generatedPhotos) {
        photoInfoDao.save(generatedPhoto).awaitFirst()
      }

      for (generatedPhoto in generatedPhotos) {
        photosRepository.reportPhoto("333", generatedPhoto.photoName)
        photosRepository.reportPhoto("444", generatedPhoto.photoName)
        photosRepository.reportPhoto("555", generatedPhoto.photoName)
      }

      val reportedPhotos = reportedPhotoDao.testFindAll().awaitFirst()
      assertEquals(6, reportedPhotos.size)

      val groupedByNamePhotos = reportedPhotos.groupBy { it.photoName }
      val groupedByUserIdPhotos = reportedPhotos.groupBy { it.userId }

      for (generatedPhoto in generatedPhotos) {
        assertEquals(3, groupedByNamePhotos[generatedPhoto.photoName]!!.size)
      }

      assertEquals(2, groupedByUserIdPhotos["333"]!!.size)
      assertEquals(2, groupedByUserIdPhotos["444"]!!.size)
      assertEquals(2, groupedByUserIdPhotos["555"]!!.size)

      for (generatedPhoto in generatedPhotos) {
        photosRepository.reportPhoto("333", generatedPhoto.photoName)
        photosRepository.reportPhoto("444", generatedPhoto.photoName)
        photosRepository.reportPhoto("555", generatedPhoto.photoName)
      }

      assertTrue(reportedPhotoDao.testFindAll().awaitFirst().isEmpty())
    }
  }
}