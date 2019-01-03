package com.kirakishou.photoexchange.database.mongo.repository

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.TestUtils
import com.kirakishou.photoexchange.TestUtils.createPhoto
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.config.ServerSettings.FILE_DIR_PATH
import com.kirakishou.photoexchange.core.IpHash
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.core.UserUuid
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotosTest : AbstractTest() {

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun `save method should create gallery photo if uploaded photo is public`() {
    dbQuery {
      usersDao.save(UserUuid("111"))
      val saved = photosRepository.save(UserId(1L), 11.1, 22.2, true, 5345L, IpHash("12121313"))

      assertEquals(saved.photoName, photosDao.testFindAll().first().photoName)
      assertEquals(saved.photoId, galleryPhotosDao.testFindAll().first().photoId)
    }
  }

  @Test
  fun `save method should NOT create gallery photo if uploaded photo is private`() {
    dbQuery {
      usersDao.save(UserUuid("111"))
      val saved = photosRepository.save(UserId(1L), 11.1, 22.2, false, 5345L, IpHash("12121313"))

      assertEquals(saved.photoName, photosDao.testFindAll().first().photoName)
      assertTrue(galleryPhotosDao.testFindAll().isEmpty())
    }
  }

  @Test
  fun `tryDoExchange should return empty PhotoInfo when there is nothing to exchange with`() {
    dbQuery {
      usersDao.save(UserUuid("111"))

      val photo = createPhoto(1L, 1L, -2L, 0L, "test", true, 11.1, 22.2, 999L, 0L, "12121313")
      val userUuid = UserUuid("1213")

      val resultPhoto = photosRepository.tryDoExchange(userUuid, photo)

      assertTrue(resultPhoto.isEmpty())
    }
  }

  @Test
  fun `tryDoExchange should return old photo when exchange was successful`() {
    dbQuery {
      val userId1 = UserUuid("111")
      val userId2 = UserUuid("222")

      usersDao.save(userId1)
      usersDao.save(userId2)

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
    dbQuery {
      usersDao.save(UserUuid("111"))
      usersDao.save(UserUuid("222"))

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
    dbQuery {
      usersDao.save(UserUuid("111"))
      usersDao.save(UserUuid("222"))

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

      assertTrue(photosDao.testFindAll().isEmpty())
      assertTrue(favouritedPhotosDao.testFindAll().isEmpty())
      assertTrue(reportedPhotosDao.testFindAll().isEmpty())
      assertTrue(locationMapsDao.testFindAll().isEmpty())
      assertTrue(galleryPhotosDao.testFindAll().isEmpty())
    }
  }

  @Test
  fun `should favourite and then unfavourite photos`() {
    dbQuery {
      val user1uuid = UserUuid("111")
      val user2uuid = UserUuid("222")
      val user3uuid = UserUuid("333")

      usersDao.save(user1uuid)
      usersDao.save(user2uuid)
      usersDao.save(user3uuid)

      val generatedPhotos = TestUtils.createExchangedPhotoPairs(2, listOf(1L, 2L))

      for (generatedPhoto in generatedPhotos) {
        photosDao.save(PhotoEntity.fromPhoto(generatedPhoto))
      }

      for (generatedPhoto in generatedPhotos) {
        photosRepository.favouritePhoto(user1uuid, generatedPhoto.photoName)
        photosRepository.favouritePhoto(user2uuid, generatedPhoto.photoName)
        photosRepository.favouritePhoto(user3uuid, generatedPhoto.photoName)
      }

      val favouritedPhotos = favouritedPhotosDao.testFindAll()
      assertEquals(6, favouritedPhotos.size)

      val groupedByPhotoIdPhotos = favouritedPhotos.groupBy { it.photoId }
      val groupedByUserIdPhotos = favouritedPhotos.groupBy { it.userId.id }

      for (generatedPhoto in generatedPhotos) {
        assertEquals(3, groupedByPhotoIdPhotos[generatedPhoto.photoId]!!.size)
      }

      assertEquals(2, groupedByUserIdPhotos[1L]!!.size)
      assertEquals(2, groupedByUserIdPhotos[2L]!!.size)
      assertEquals(2, groupedByUserIdPhotos[3L]!!.size)

      for (generatedPhoto in generatedPhotos) {
        photosRepository.favouritePhoto(user1uuid, generatedPhoto.photoName)
        photosRepository.favouritePhoto(user2uuid, generatedPhoto.photoName)
        photosRepository.favouritePhoto(user3uuid, generatedPhoto.photoName)
      }

      assertTrue(favouritedPhotosDao.testFindAll().isEmpty())
    }
  }

  @Test
  fun `should report and then unreport photos`() {
    dbQuery {
      val user1uuid = UserUuid("111")
      val user2uuid = UserUuid("222")
      val user3uuid = UserUuid("333")

      usersDao.save(user1uuid)
      usersDao.save(user2uuid)
      usersDao.save(user3uuid)

      val generatedPhotos = TestUtils.createExchangedPhotoPairs(2, listOf(1L, 2L))

      for (generatedPhoto in generatedPhotos) {
        photosDao.save(PhotoEntity.fromPhoto(generatedPhoto))
      }

      for (generatedPhoto in generatedPhotos) {
        photosRepository.reportPhoto(user1uuid, generatedPhoto.photoName)
        photosRepository.reportPhoto(user2uuid, generatedPhoto.photoName)
        photosRepository.reportPhoto(user3uuid, generatedPhoto.photoName)
      }

      val reportedPhotos = reportedPhotosDao.testFindAll()
      assertEquals(6, reportedPhotos.size)

      val groupedByPhotoIdPhotos = reportedPhotos.groupBy { it.photoId }
      val groupedByUserIdPhotos = reportedPhotos.groupBy { it.userId.id }

      for (generatedPhoto in generatedPhotos) {
        assertEquals(3, groupedByPhotoIdPhotos[generatedPhoto.photoId]!!.size)
      }

      assertEquals(2, groupedByUserIdPhotos[1L]!!.size)
      assertEquals(2, groupedByUserIdPhotos[2L]!!.size)
      assertEquals(2, groupedByUserIdPhotos[3L]!!.size)

      for (generatedPhoto in generatedPhotos) {
        photosRepository.reportPhoto(user1uuid, generatedPhoto.photoName)
        photosRepository.reportPhoto(user2uuid, generatedPhoto.photoName)
        photosRepository.reportPhoto(user3uuid, generatedPhoto.photoName)
      }

      assertTrue(reportedPhotosDao.testFindAll().isEmpty())
    }
  }
}