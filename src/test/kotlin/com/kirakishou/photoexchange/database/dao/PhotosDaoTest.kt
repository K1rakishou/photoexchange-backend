package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.AbstractTest
import com.kirakishou.photoexchange.core.UserId
import org.joda.time.DateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PhotosDaoTest : AbstractTest() {

  @Before
  override fun setUp() {
    super.setUp()
  }
 
  @After
  override fun tearDown() {
    super.tearDown()
  }

  @Test
  fun `test countFreshUploadedPhotosSince should return 0 when DB is empty`() {
    dbQuery {
      assertEquals(0, photosDao.countFreshUploadedPhotosSince(UserId(1), DateTime(1000)))
    }
  }

}