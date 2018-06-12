package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import reactor.core.publisher.Mono

@RunWith(SpringJUnit4ClassRunner::class)
class PhotoInfoRepositoryTest : AbstractRepositoryTest() {

	@Before
	fun setup() {
		super.init()
	}

	@After
	fun tearDown() {
		super.clear()
	}

	@Test
	fun `delete photo info exchange record from the database when something happened in first part of photo exchange`() {
		runBlocking {
			val photoInfo = PhotoInfo.create("111", "photo_name", true, 11.1, 11.1, 1500L)

			Mockito.`when`(photoInfoDao.updateSetExchangeId(1L, 1L))
				.thenReturn(Mono.just(false))

			val newPhotoInfo = photoInfoRepository.save(photoInfo)

			assertEquals(false, newPhotoInfo.isEmpty())
			assertEquals(false, photoInfoRepository.tryDoExchange(newPhotoInfo))
			assertEquals(true, photoInfoExchangeDao.findById(1).awaitFirst().isEmpty())
		}
	}

	@Test
	fun `delete photo info exchange record from the database when something happened in the second part of photo exchange`() {
		runBlocking {
			val photoInfo1 = PhotoInfo(1, 1, -1L, "111", "", "photo_name", true, 11.1, 11.1, 1500L)
			val photoInfo2 = PhotoInfo(2, -1, -1L, "222", "", "photo_name2", true, 22.2, 22.2, 1505L)
			val photoInfoExchange = PhotoInfoExchange(1, 1, -1, "111", "", 1500L)

			photoInfoDao.save(photoInfo1).awaitFirst()
			photoInfoDao.save(photoInfo2).awaitFirst()
			photoInfoExchangeDao.save(photoInfoExchange).awaitFirst()

			Mockito.`when`(photoInfoDao.updateSetReceiverId(2, "111"))
				.thenReturn(Mono.just(false))

			assertEquals(false, photoInfoRepository.tryDoExchange(photoInfo2))

			val resultPhotoInfo1 = photoInfoDao.findById(1).awaitFirst()
			val resultPhotoInfo2 = photoInfoDao.findById(2).awaitFirst()
			val resultPhotoInfoExchange = photoInfoExchangeDao.findById(1).awaitFirst()

			assertEquals("", resultPhotoInfo1.receiverUserId)
			assertEquals(-1L, resultPhotoInfo2.exchangeId)
			assertEquals(-1L, resultPhotoInfoExchange.receiverPhotoId)
			assertEquals("", resultPhotoInfoExchange.receiverUserId)
		}
	}
}