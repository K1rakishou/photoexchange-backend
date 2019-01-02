package com.kirakishou.photoexchange.service

import com.google.gson.Gson
import com.kirakishou.photoexchange.TestDatabaseFactory
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.LocationMapRepository
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.database.repository.UsersRepository
import kotlinx.coroutines.Dispatchers
import org.mockito.Mockito

abstract class AbstractServiceTest {
  private val database = TestDatabaseFactory().db

  lateinit var photosDao: PhotosDao
  lateinit var usersDao: UsersDao
  lateinit var galleryPhotosDao: GalleryPhotosDao
  lateinit var favouritedPhotosDao: FavouritedPhotosDao
  lateinit var reportedPhotosDao: ReportedPhotosDao
  lateinit var locationMapsDao: LocationMapsDao
  lateinit var bansDao: BansDao

  lateinit var generator: GeneratorService
  lateinit var diskManipulationService: DiskManipulationService
  lateinit var jsonConverterService: JsonConverterService
  lateinit var webClientService: WebClientService
  lateinit var googleCredentialsService: GoogleCredentialsService

  lateinit var usersRepository: UsersRepository
  lateinit var photosRepository: PhotosRepository
  lateinit var locationMapRepository: LocationMapRepository

  open fun setUp() {
    photosDao = Mockito.spy(PhotosDao())
    usersDao = Mockito.spy(UsersDao())
    galleryPhotosDao = Mockito.spy(GalleryPhotosDao())
    favouritedPhotosDao = Mockito.spy(FavouritedPhotosDao())
    reportedPhotosDao = Mockito.spy(ReportedPhotosDao())
    locationMapsDao = Mockito.spy(LocationMapsDao())
    bansDao = Mockito.spy(BansDao())

    generator = Mockito.spy(GeneratorService())
    diskManipulationService = Mockito.spy(DiskManipulationService())
    webClientService = Mockito.mock(WebClientService::class.java)
    googleCredentialsService = Mockito.mock(GoogleCredentialsService::class.java)
    jsonConverterService = Mockito.spy(
      JsonConverterService(Gson().newBuilder().create())
    )

    usersRepository = Mockito.spy(
      UsersRepository(
        usersDao,
        generator,
        database,
        Dispatchers.Unconfined
      )
    )

    photosRepository = Mockito.spy(
      PhotosRepository(
        photosDao,
        usersDao,
        galleryPhotosDao,
        favouritedPhotosDao,
        reportedPhotosDao,
        locationMapsDao,
        generator,
        diskManipulationService,
        database,
        Dispatchers.Unconfined
      )
    )

    locationMapRepository = Mockito.spy(
      LocationMapRepository(
        locationMapsDao,
        photosDao,
        database,
        Dispatchers.Unconfined
      )
    )
  }

  open fun tearDown() {
  }


}