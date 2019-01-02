package com.kirakishou.photoexchange.database.mongo.repository

import com.kirakishou.photoexchange.TestUtils
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.config.ServerSettings.FILE_DIR_PATH
import com.kirakishou.photoexchange.core.DatabaseTransactionException
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import com.nhaarman.mockito_kotlin.any
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
    val photo = createPhoto(1L, 1L, -2L, 0L, "test", true, 11.1, 22.2, 999L, 0L, "12121313")

    runBlocking {
      Mockito
        .doReturn(Mono.error<Boolean>(DatabaseTransactionException("BAM")))
        .`when`(reportedPhotosDao).deleteAllFavouritesByPhotoId(any())

      val saved = photosDao.save(PhotoEntity.fromPhoto(photo))
      galleryPhotosDao.save(saved.photoId, 777L)
      reportedPhotosDao.reportPhoto(saved.photoId, saved.userId)
      favouritedPhotosDao.favouritePhoto(saved.photoId, saved.userId)
      locationMapsDao.save(photo.photoId)

      assertFalse(photosRepository.delete(photo.photoId, photo.photoName))

      assertEquals(1, photosDao.testFindAll().size)
      assertEquals(photo.photoName, photosDao.testFindAll().first().photoName)
      assertEquals(photo.photoId, favouritedPhotosDao.testFindAll().first().photoId)
      assertEquals(photo.photoId, reportedPhotosDao.testFindAll().first().photoId)
      assertEquals(photo.photoId, galleryPhotosDao.testFindAll().first().photoId)
      assertEquals(photo.photoId, locationMapsDao.testFindAll().first().photoId)
    }
  }

  @Test
  fun `save method should create gallery photo if uploaded photo is public`() {
    val photo = createPhoto(1L, 1L, -2L, 0L, "test", true, 11.1, 22.2, 999L, 0L, "12121313")

    runBlocking {
      val saved = photosRepository.save(photo.userId, 11.1, 22.2, true, 5345L, photo.ipHash)

      assertEquals(saved.photoName, photosDao.testFindAll().first().photoName)
      assertEquals(saved.photoId, galleryPhotosDao.testFindAll().first().photoId)
    }
  }

  @Test
  fun `save method should NOT create gallery photo if uploaded photo is private`() {
    val photo = createPhoto(1L, 1L, -2L, 0L, "test", true, 11.1, 22.2, 999L, 0L, "12121313")

    runBlocking {
      val saved = photosRepository.save(photo.userId, 11.1, 22.2, true, 5345L, photo.ipHash)

      assertEquals(saved.photoName, photosDao.testFindAll().first().photoName)
      assertTrue(galleryPhotosDao.testFindAll().isEmpty())
    }
  }

  @Test
  fun `tryDoExchange should return empty PhotoInfo when there is nothing to exchange with`() {
    runBlocking {
      val photo = createPhoto(1L, 1L, -2L, 0L, "test", true, 11.1, 22.2, 999L, 0L, "12121313")
      val userUuid = UserUuid("1213")

      val resultPhoto = photosRepository.tryDoExchange(userUuid, photo)

      assertTrue(resultPhoto.isEmpty())
    }
  }

  @Test
  fun `tryDoExchange should return old photo when exchange was successful`() {
    runBlocking {
      val userId1 = UserUuid("111")
      val userId2 = UserUuid("222")

      val photo1 = createPhoto(1L, 1L, -2L, 1L, "ert", true, 11.1, 22.2, 5345L, 0L, "23123")
      val photo2 = createPhoto(2L, 2L, -2L, 2L, "ttt", true, 11.1, 22.2, 5345L, 0L, "23123")

      photosDao.save(PhotoEntity.fromPhoto(photo1))
      photosDao.save(PhotoEntity.fromPhoto(photo2))

      val resultPhoto = photosRepository.tryDoExchange(userId2, photo2)

      assertEquals(1L, resultPhoto.photoId.id)
      assertEquals(2L, resultPhoto.exchangedPhotoId.id)
    }
  }

  @Test
  fun `should mark as deleted 4 photos`() {
    runBlocking {
      val generatedPhotos = TestUtils.createExchangedPhotoPairs(10, listOf(1L, 2L))

      for (generatedPhoto in generatedPhotos) {
        photosDao.save(PhotoEntity.fromPhoto(generatedPhoto))
      }

      val count = photosRepository.markAsDeletedPhotosUploadedEarlierThan(
        104L,
        999L,
        100
      )

      assertEquals(4, count)

      val found = photosDao.testFindAll()
      assertEquals(4, found.count { it.deletedOn == 999L })
    }
  }

  @Test
  fun `should delete all photos with deletedOn field greater than zero`() {
    runBlocking {
      val generatedPhotos = TestUtils.createExchangedPhotoPairs(10, listOf(1L, 2L))
        .map { it.copy(deletedOn = 111L) }

      for (generatedPhoto in generatedPhotos) {
        photosDao.save(PhotoEntity.fromPhoto(generatedPhoto))

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
      assertTrue(photosDao.testFindAll().isEmpty())
    }
  }

  @Test
  fun `should favourite and then unfavourite photos`() {
    runBlocking {
      val generatedPhotos = TestUtils.createExchangedPhotoPairs(2, listOf(1L, 2L))

      for (generatedPhoto in generatedPhotos) {
        photosDao.save(PhotoEntity.fromPhoto(generatedPhoto))
      }

      for (generatedPhoto in generatedPhotos) {
        photosRepository.favouritePhoto(UserId(3L), generatedPhoto.photoName)
        photosRepository.favouritePhoto(UserId(4L), generatedPhoto.photoName)
        photosRepository.favouritePhoto(UserId(5L), generatedPhoto.photoName)
      }

      val favouritedPhotos = favouritedPhotosDao.testFindAll()
      assertEquals(6, favouritedPhotos.size)

      val groupedByPhotoIdPhotos = favouritedPhotos.groupBy { it.photoId }
      val groupedByUserIdPhotos = favouritedPhotos.groupBy { it.userId }

      for (generatedPhoto in generatedPhotos) {
        assertEquals(3, groupedByPhotoIdPhotos[generatedPhoto.photoId]!!.size)
      }

      assertEquals(2, groupedByUserIdPhotos[UserId(3L)]!!.size)
      assertEquals(2, groupedByUserIdPhotos[UserId(4L)]!!.size)
      assertEquals(2, groupedByUserIdPhotos[UserId(5L)]!!.size)

      for (generatedPhoto in generatedPhotos) {
        photosRepository.favouritePhoto(UserId(3L), generatedPhoto.photoName)
        photosRepository.favouritePhoto(UserId(4L), generatedPhoto.photoName)
        photosRepository.favouritePhoto(UserId(5L), generatedPhoto.photoName)
      }

      assertTrue(favouritedPhotosDao.testFindAll().isEmpty())
    }
  }

  @Test
  fun `should report and then unreport photos`() {
    runBlocking {
      val generatedPhotos = TestUtils.createExchangedPhotoPairs(2, listOf(1L, 2L))

      for (generatedPhoto in generatedPhotos) {
        photosDao.save(PhotoEntity.fromPhoto(generatedPhoto))
      }

      for (generatedPhoto in generatedPhotos) {
        photosRepository.reportPhoto(UserId(3L), generatedPhoto.photoName)
        photosRepository.reportPhoto(UserId(4L), generatedPhoto.photoName)
        photosRepository.reportPhoto(UserId(5L), generatedPhoto.photoName)
      }

      val reportedPhotos = reportedPhotosDao.testFindAll()
      assertEquals(6, reportedPhotos.size)

      val groupedByPhotoIdPhotos = reportedPhotos.groupBy { it.photoId }
      val groupedByUserIdPhotos = reportedPhotos.groupBy { it.userId }

      for (generatedPhoto in generatedPhotos) {
        assertEquals(3, groupedByPhotoIdPhotos[generatedPhoto.photoId]!!.size)
      }

      assertEquals(2, groupedByUserIdPhotos[UserId(3L)]!!.size)
      assertEquals(2, groupedByUserIdPhotos[UserId(4L)]!!.size)
      assertEquals(2, groupedByUserIdPhotos[UserId(5L)]!!.size)

      for (generatedPhoto in generatedPhotos) {
        photosRepository.reportPhoto(UserId(3L), generatedPhoto.photoName)
        photosRepository.reportPhoto(UserId(4L), generatedPhoto.photoName)
        photosRepository.reportPhoto(UserId(5L), generatedPhoto.photoName)
      }

      assertTrue(reportedPhotosDao.testFindAll().isEmpty())
    }
  }
}