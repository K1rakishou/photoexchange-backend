package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.service.GeneratorService
import com.kirakishou.photoexchange.service.concurrency.TestConcurrencyService
import com.mongodb.ConnectionString
import org.mockito.Mockito
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory

abstract class AbstractRepositoryTest {

	lateinit var template: ReactiveMongoTemplate

	lateinit var mongoSequenceDao: MongoSequenceDao
	lateinit var photoInfoDao: PhotoInfoDao
	lateinit var photoInfoExchangeDao: PhotoInfoExchangeDao
	lateinit var galleryPhotoDao: GalleryPhotoDao
	lateinit var favouritedPhotoDao: FavouritedPhotoDao
	lateinit var reportedPhotoDao: ReportedPhotoDao
	lateinit var userInfoDao: UserInfoDao
	lateinit var locationMapDao: LocationMapDao

	lateinit var photoInfoRepository: PhotoInfoRepository

	fun init() {
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
		locationMapDao = Mockito.spy(LocationMapDao(template).also {
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
			locationMapDao,
			generator,
			concurrentService)
		)
	}

	fun clear() {
		mongoSequenceDao.clear()
		photoInfoDao.clear()
		photoInfoExchangeDao.clear()
		galleryPhotoDao.clear()
		favouritedPhotoDao.clear()
		reportedPhotoDao.clear()
		userInfoDao.clear()
	}

}