package com.kirakishou.photoexchange.service

import com.google.gson.Gson
import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.LocationMapRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.mongodb.ConnectionString
import kotlinx.coroutines.Dispatchers
import org.mockito.Mockito
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory

abstract class AbstractServiceTest {

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
  lateinit var userInfoDao: UserInfoDao

  lateinit var generator: GeneratorService
  lateinit var diskManipulationService: DiskManipulationService
  lateinit var jsonConverterService: JsonConverterService
  lateinit var webClientService: WebClientService
  lateinit var googleCredentialsService: GoogleCredentialsService

  lateinit var userInfoRepository: UserInfoRepository
  lateinit var photoInfoRepository: PhotoInfoRepository
  lateinit var locationMapRepository: LocationMapRepository

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
    userInfoDao = Mockito.spy(UserInfoDao(template).apply {
      clear()
      create()
    })

    generator = Mockito.spy(GeneratorService())
    diskManipulationService = Mockito.spy(DiskManipulationService())
    webClientService = Mockito.mock(WebClientService::class.java)
    googleCredentialsService = Mockito.mock(GoogleCredentialsService::class.java)
    jsonConverterService = Mockito.spy(
      JsonConverterService(Gson().newBuilder().create())
    )

    userInfoRepository = Mockito.spy(
      UserInfoRepository(
        mongoSequenceDao,
        userInfoDao,
        generator,
        Dispatchers.Unconfined
      )
    )

    photoInfoRepository = Mockito.spy(
      PhotoInfoRepository(
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
    )

    locationMapRepository = Mockito.spy(
      LocationMapRepository(
        template,
        mongoSequenceDao,
        locationMapDao,
        photoInfoDao,
        Dispatchers.Unconfined
      )
    )
  }

  open fun tearDown() {
    photoInfoDao.clear()
    mongoSequenceDao.clear()
    galleryPhotoDao.clear()
    favouritedPhotoDao.clear()
    reportedPhotoDao.clear()
    locationMapDao.clear()
    userInfoDao.clear()
  }


}