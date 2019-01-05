package com.kirakishou.photoexchange.database.pgsql.repository

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.TestUtils
import com.kirakishou.photoexchange.TestUtils.createPhoto
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.config.ServerSettings.FILE_DIR_PATH
import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.core.io.ClassPathResource
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.File
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotosRepositoryTest : AbstractTest() {

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
    }

    runBlocking {
      val saved = photosRepository.save(UserId(1L), 11.1, 22.2, true, 5345L, IpHash("12121313"))

      assertEquals(saved.photoName, dbQuery { photosDao.testFindAll() }.first().photoName)
      assertEquals(saved.photoId, dbQuery { galleryPhotosDao.testFindAll() }.first().photoId)
    }
  }

  @Test
  fun `save method should NOT create gallery photo if uploaded photo is private`() {
    dbQuery {
      usersDao.save(UserUuid("111"))
    }

    runBlocking {
      val saved = photosRepository.save(UserId(1L), 11.1, 22.2, false, 5345L, IpHash("12121313"))

      assertEquals(saved.photoName, dbQuery { photosDao.testFindAll() }.first().photoName)
      assertTrue(dbQuery { galleryPhotosDao.testFindAll() }.isEmpty())
    }
  }

  @Test
  fun `tryDoExchange should return empty PhotoInfo when there is nothing to exchange with`() {
    dbQuery {
      usersDao.save(UserUuid("111"))

      val photo = createPhoto(
        1L,
        ExchangeState.ReadyToExchange,
        1L,
        -2L,
        0L,
        "test",
        true,
        11.1,
        22.2,
        999L,
        0L,
        "12121313"
      )
      val userUuid = UserUuid("1213")

      val resultPhoto = photosRepository.tryDoExchange(userUuid, photo)

      assertTrue(resultPhoto.isEmpty())
    }
  }

  @Test
  fun `tryDoExchange should return old photo when exchange was successful`() {
    val userId1 = UserUuid("111")
    val userId2 = UserUuid("222")

    val photo1 = createPhoto(
      1L,
      ExchangeState.ReadyToExchange,
      1L,
      -1L,
      1L,
      "ert",
      true,
      11.1,
      22.2,
      5345L,
      0L,
      "23123"
    )
    val photo2 = createPhoto(
      2L,
      ExchangeState.ReadyToExchange,
      2L,
      -1L,
      2L,
      "ttt",
      true,
      11.1,
      22.2,
      5345L,
      0L,
      "23123"
    )

    dbQuery {
      usersDao.save(userId1)
      usersDao.save(userId2)

      photosDao.save(PhotoEntity.fromPhoto(photo1))
      photosDao.save(PhotoEntity.fromPhoto(photo2))
    }

    runBlocking {
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
    }

    runBlocking {
      val count = photosRepository.markAsDeletedPhotosUploadedEarlierThan(
        104L,
        999L,
        100
      )

      assertEquals(4, count)

      val found = dbQuery { photosDao.testFindAll() }
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
    }

    dbQuery {
      photosRepository.cleanDatabaseAndPhotos(200L, 100)

      assertTrue(File(FILE_DIR_PATH).list().isEmpty())

      assertTrue(dbQuery { photosDao.testFindAll() }.isEmpty())
      assertTrue(dbQuery { favouritedPhotosDao.testFindAll() }.isEmpty())
      assertTrue(dbQuery { reportedPhotosDao.testFindAll() }.isEmpty())
      assertTrue(dbQuery { locationMapsDao.testFindAll() }.isEmpty())
      assertTrue(dbQuery { galleryPhotosDao.testFindAll() }.isEmpty())
    }
  }

  @Test
  fun `should favourite and then unfavourite photos`() {
    val user1uuid = UserUuid("111")
    val user2uuid = UserUuid("222")
    val user3uuid = UserUuid("333")

    val generatedPhotos = TestUtils.createExchangedPhotoPairs(2, listOf(1L, 2L))

    dbQuery {
      usersDao.save(user1uuid)
      usersDao.save(user2uuid)
      usersDao.save(user3uuid)

      for (generatedPhoto in generatedPhotos) {
        photosDao.save(PhotoEntity.fromPhoto(generatedPhoto))
      }
    }

    runBlocking {
      for (generatedPhoto in generatedPhotos) {
        photosRepository.favouritePhoto(user1uuid, generatedPhoto.photoName)
        photosRepository.favouritePhoto(user2uuid, generatedPhoto.photoName)
        photosRepository.favouritePhoto(user3uuid, generatedPhoto.photoName)
      }

      val favouritedPhotos = dbQuery { favouritedPhotosDao.testFindAll() }
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

      assertTrue(dbQuery { favouritedPhotosDao.testFindAll() }.isEmpty())
    }
  }

  @Test
  fun `should report and then unreport photos`() {
    val user1uuid = UserUuid("111")
    val user2uuid = UserUuid("222")
    val user3uuid = UserUuid("333")

    val generatedPhotos = TestUtils.createExchangedPhotoPairs(2, listOf(1L, 2L))

    dbQuery {
      usersDao.save(user1uuid)
      usersDao.save(user2uuid)
      usersDao.save(user3uuid)

      for (generatedPhoto in generatedPhotos) {
        photosDao.save(PhotoEntity.fromPhoto(generatedPhoto))
      }
    }

    runBlocking {
      for (generatedPhoto in generatedPhotos) {
        photosRepository.reportPhoto(user1uuid, generatedPhoto.photoName)
        photosRepository.reportPhoto(user2uuid, generatedPhoto.photoName)
        photosRepository.reportPhoto(user3uuid, generatedPhoto.photoName)
      }

      val reportedPhotos = dbQuery { reportedPhotosDao.testFindAll() }
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

      assertTrue(dbQuery { reportedPhotosDao.testFindAll() }.isEmpty())
    }
  }

  @Test
  fun `test favourite unfavourite photo concurrently 500 times`() {
    val concurrency = 500
    val photoName = "1212"
    val userUuidList = (2 until concurrency + 2)
      .map { UserUuid(it.toString()) }
    val photo = createPhoto(1L, ExchangeState.Exchanged, 1L, 1L, 1L, photoName, true, 11.1, 22.2, 444L, 0L, "45345")

    dbQuery {
      for (userUuid in userUuidList) {
        usersDao.save(userUuid)
      }

      photosDao.save(PhotoEntity.fromPhoto(photo))
    }

    val executor = Executors.newFixedThreadPool(40)

    Flux.fromIterable(userUuidList.shuffled())
      .flatMap {
        return@flatMap Flux.just(it)
          .subscribeOn(Schedulers.fromExecutor(executor))
          .map { uuid ->
            println("Executing request for uuid $uuid")

            runBlocking { photosRepository.favouritePhoto(uuid, PhotoName(photoName)) }
          }
      }
      .collectList()
      .block()

    kotlin.run {
      val allFavourites = dbQuery { favouritedPhotosDao.testFindAll() }
      assertEquals(concurrency, allFavourites.size)

      assertEquals(concurrency, allFavourites.distinctBy { it.userId.id }.size)
      assertEquals(concurrency, allFavourites.distinctBy { it.favouritedPhotoId.id }.size)
      assertEquals(1, allFavourites.distinctBy { it.photoId.id }.size)
    }

    Flux.fromIterable(userUuidList.shuffled())
      .flatMap {
        return@flatMap Flux.just(it)
          .subscribeOn(Schedulers.fromExecutor(executor))
          .map { uuid ->
            println("Executing request for uuid $uuid")

            runBlocking { photosRepository.favouritePhoto(uuid, PhotoName(photoName)) }
          }
      }
      .collectList()
      .block()

    kotlin.run {
      val allFavourites = dbQuery { favouritedPhotosDao.testFindAll() }
      assertEquals(0, allFavourites.size)
    }
  }

  @Test
  fun `test report unreport photo concurrently 500 times`() {
    val concurrency = 500
    val photoName = "1212"
    val userUuidList = (2 until concurrency + 2)
      .map { UserUuid(it.toString()) }
    val photo = createPhoto(1L, ExchangeState.Exchanged, 1L, 1L, 1L, photoName, true, 11.1, 22.2, 444L, 0L, "45345")

    dbQuery {
      for (userUuid in userUuidList) {
        usersDao.save(userUuid)
      }

      photosDao.save(PhotoEntity.fromPhoto(photo))
    }

    val executor = Executors.newFixedThreadPool(40)

    Flux.fromIterable(userUuidList.shuffled())
      .flatMap {
        return@flatMap Flux.just(it)
          .subscribeOn(Schedulers.fromExecutor(executor))
          .map { uuid ->
            println("Executing request for uuid $uuid")

            runBlocking { photosRepository.reportPhoto(uuid, PhotoName(photoName)) }
          }
      }
      .collectList()
      .block()

    kotlin.run {
      val allReports = dbQuery { reportedPhotosDao.testFindAll() }
      assertEquals(concurrency, allReports.size)

      assertEquals(concurrency, allReports.distinctBy { it.userId.id }.size)
      assertEquals(concurrency, allReports.distinctBy { it.reportedPhotoId.id }.size)
      assertEquals(1, allReports.distinctBy { it.photoId.id }.size)
    }

    Flux.fromIterable(userUuidList.shuffled())
      .flatMap {
        return@flatMap Flux.just(it)
          .subscribeOn(Schedulers.fromExecutor(executor))
          .map { uuid ->
            println("Executing request for uuid $uuid")

            runBlocking { photosRepository.reportPhoto(uuid, PhotoName(photoName)) }
          }
      }
      .collectList()
      .block()

    kotlin.run {
      val allReports = dbQuery { reportedPhotosDao.testFindAll() }
      assertEquals(0, allReports.size)
    }
  }
}