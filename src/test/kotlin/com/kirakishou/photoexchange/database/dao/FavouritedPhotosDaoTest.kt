package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.entity.PhotoEntity
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FavouritedPhotosDaoTest : AbstractTest() {

  @Before
  override fun setUp() {
    super.setUp()
  }

  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun `test countFavouritesByPhotoIdList should return list of photo ids with their counts`() {
    dbQuery {
      usersDao.save(UserUuid("111"))
      usersDao.save(UserUuid("222"))

      for (i in 3 until 23) {
        usersDao.save(UserUuid(i.toString()))
      }

      photosDao.save(
        PhotoEntity(
          PhotoId(1L),
          ExchangeState.Exchanged,
          UserId(1L),
          ExchangedPhotoId(2L),
          LocationMapId(1L),
          PhotoName("123"),
          true,
          11.1,
          22.2,
          DateTime(444L),
          DateTime(0L),
          IpHash("123")
        )
      )
      photosDao.save(
        PhotoEntity(
          PhotoId(2L),
          ExchangeState.Exchanged,
          UserId(2L),
          ExchangedPhotoId(1L),
          LocationMapId(2L),
          PhotoName("223"),
          true,
          11.1,
          22.2,
          DateTime(444L),
          DateTime(0L),
          IpHash("123")
        )
      )
    }

    dbQuery {
      for (i in 3 until 13) {
        favouritedPhotosDao.favouritePhoto(PhotoId(1), UserId(i.toLong()))
      }

      for (i in 13 until 23) {
        favouritedPhotosDao.favouritePhoto(PhotoId(2), UserId(i.toLong()))
      }
    }

    dbQuery {
      val map = favouritedPhotosDao.countFavouritesByPhotoIdList(listOf(PhotoId(1), PhotoId(2)))
      val allFavourites = favouritedPhotosDao.testFindAll()

      assertEquals(2, map.size)
      assertEquals(10, map[1L]!!)
      assertEquals(10, map[2L]!!)

      assertEquals(20, allFavourites.size)

      assertEquals(10, allFavourites.count { it.photoId.id == 1L })
      assertEquals(10, allFavourites.count { it.photoId.id == 2L })
    }
  }
}