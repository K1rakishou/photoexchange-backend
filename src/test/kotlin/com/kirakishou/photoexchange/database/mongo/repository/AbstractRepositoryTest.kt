package com.kirakishou.photoexchange.database.mongo.repository

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.mongo.dao.*
import com.kirakishou.photoexchange.database.pgsql.repository.PhotosRepository
import com.kirakishou.photoexchange.service.DiskManipulationService
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.Dispatchers
import org.mockito.Mockito

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

	lateinit var photosRepository: PhotosRepository

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

		photosRepository = PhotosRepository(
      template,
      mongoSequenceDao,
      photoInfoDao,
      galleryPhotoDao,
      favouritedPhotoDao,
      reportedPhotoDao,
      locationMapDao,
      generator,
      diskManipulationService,
      Dispatchers.Unconfined
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