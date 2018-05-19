package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.model.repo.PhotoInfoExchange
import com.kirakishou.photoexchange.service.GeneratorService
import com.kirakishou.photoexchange.service.concurrency.TestConcurrencyService
import com.mongodb.ConnectionString
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import reactor.core.publisher.Mono

@RunWith(SpringJUnit4ClassRunner::class)
class PhotoInfoRepositoryTest {
	lateinit var photoInfoRepository: PhotoInfoRepository

	lateinit var template: ReactiveMongoTemplate
	lateinit var mongoSequenceDao: MongoSequenceDao
	lateinit var photoInfoDao: PhotoInfoDao
	lateinit var photoInfoExchangeDao: PhotoInfoExchangeDao
	lateinit var galleryPhotoDao: GalleryPhotoDao
	lateinit var favouritedPhotoDao: FavouritedPhotoDao
	lateinit var reportedPhotoDao: ReportedPhotoDao
	lateinit var userInfoDao: UserInfoDao

	@Test
	fun `test try do exchange`() {
		runBlocking {
			createPhotoInfoRepository()

			Mockito.doReturn(Mono.just(PhotoInfoExchange.empty())).`when`(photoInfoExchangeDao)
				.tryDoExchangeWithOldestPhoto(Mockito.anyLong(), Mockito.anyString())

			val result = photoInfoRepository.tryDoExchange(PhotoInfo.empty())

			println()
			println()
			println()
			println()
			println()
			println()
		}
	}

	private fun createPhotoInfoRepository() {
		template = ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(
			ConnectionString("mongodb://${ServerSettings.DatabaseInfo.HOST}:${ServerSettings.DatabaseInfo.PORT}/photoexchange_test"))
		)

		mongoSequenceDao = Mockito.spy(MongoSequenceDao(template).also {
			it.clear()
			it.create()
		})
		photoInfoDao = Mockito.spy(PhotoInfoDao(template).also {
			it.clear()
			it.create()
		})
		photoInfoExchangeDao = Mockito.spy(PhotoInfoExchangeDao(template).also {
			it.clear()
			it.create()
		})
		galleryPhotoDao = Mockito.spy(GalleryPhotoDao(template).also {
			it.clear()
			it.create()
		})
		favouritedPhotoDao = Mockito.spy(FavouritedPhotoDao(template).also {
			it.clear()
			it.create()
		})
		reportedPhotoDao = Mockito.spy(ReportedPhotoDao(template).also {
			it.clear()
			it.create()
		})
		userInfoDao = Mockito.spy(UserInfoDao(template).also {
			it.clear()
			it.create()
		})

		val generator = Mockito.spy(GeneratorService())
		val concurrentService = TestConcurrencyService()

		photoInfoRepository = Mockito.spy(PhotoInfoRepository(
			mongoSequenceDao,
			photoInfoDao,
			photoInfoExchangeDao,
			galleryPhotoDao,
			favouritedPhotoDao,
			reportedPhotoDao,
			userInfoDao,
			generator,
			concurrentService)
		)
	}
}