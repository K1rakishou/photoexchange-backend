package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.service.DiskManipulationService
import com.kirakishou.photoexchange.service.GeneratorService
import com.mongodb.ConnectionString
import org.mockito.Mockito
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory

abstract class AbstractRepositoryTest {

	val template = ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(
		ConnectionString("mongodb://${ServerSettings.TestDatabaseInfo.HOST}:" +
			"${ServerSettings.TestDatabaseInfo.PORT}/${ServerSettings.TestDatabaseInfo.DB_NAME}"))
	)

	lateinit var photoInfoDao: PhotoInfoDao
	lateinit var mongoSequenceDao: MongoSequenceDao
	lateinit var galleryPhotoDao: GalleryPhotoDao
	lateinit var favouritedPhotoDao: FavouritedPhotoDao
	lateinit var reportedPhotoDao: ReportedPhotoDao
	lateinit var locationMapDao: LocationMapDao
	lateinit var generator: GeneratorService
	lateinit var diskManipulationService: DiskManipulationService

	lateinit var photoInfoRepository: PhotoInfoRepository

	open fun setUp() {
		photoInfoDao = Mockito.spy(PhotoInfoDao(template).apply {
			clear()
			create()
		})
		mongoSequenceDao = Mockito.spy(MongoSequenceDao(template).apply {
			clear()
			create()
		})
		galleryPhotoDao = Mockito.spy(GalleryPhotoDao(template).apply {
			clear()
			create()
		})
		favouritedPhotoDao = Mockito.spy(FavouritedPhotoDao(template).apply {
			clear()
			create()
		})
		reportedPhotoDao = Mockito.spy(ReportedPhotoDao(template).apply {
			clear()
			create()
		})
		locationMapDao = Mockito.spy(LocationMapDao(template).apply {
			clear()
			create()
		})

		generator = Mockito.spy(GeneratorService())
		diskManipulationService = Mockito.spy(DiskManipulationService())

		photoInfoRepository = PhotoInfoRepository(
			template,
			mongoSequenceDao,
			photoInfoDao,
			galleryPhotoDao,
			favouritedPhotoDao,
			reportedPhotoDao,
			locationMapDao,
			generator,
			diskManipulationService
		)
	}

	open fun tearDown() {
		photoInfoDao.clear()
		mongoSequenceDao.clear()
		galleryPhotoDao.clear()
		favouritedPhotoDao.clear()
		reportedPhotoDao.clear()
		locationMapDao.clear()
	}

}