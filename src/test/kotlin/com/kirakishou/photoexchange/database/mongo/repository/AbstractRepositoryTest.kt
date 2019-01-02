package com.kirakishou.photoexchange.database.mongo.repository

import com.kirakishou.photoexchange.TestDatabaseFactory
import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.PhotosRepository
import com.kirakishou.photoexchange.service.DiskManipulationService
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.Dispatchers
import org.mockito.Mockito

abstract class AbstractRepositoryTest {
  private val database = TestDatabaseFactory().db

  lateinit var photosDao: PhotosDao
  lateinit var usersDao: UsersDao
  lateinit var galleryPhotosDao: GalleryPhotosDao
  lateinit var favouritedPhotosDao: FavouritedPhotosDao
  lateinit var reportedPhotosDao: ReportedPhotosDao
  lateinit var locationMapsDao: LocationMapsDao
  lateinit var generator: GeneratorService
  lateinit var diskManipulationService: DiskManipulationService

  lateinit var photosRepository: PhotosRepository

  open fun setUp() {
    photosDao = Mockito.spy(PhotosDao())
    usersDao = Mockito.spy(UsersDao())
    galleryPhotosDao = Mockito.spy(GalleryPhotosDao())
    favouritedPhotosDao = Mockito.spy(FavouritedPhotosDao())
    reportedPhotosDao = Mockito.spy(ReportedPhotosDao())
    locationMapsDao = Mockito.spy(LocationMapsDao())

    generator = Mockito.spy(GeneratorService())
    diskManipulationService = Mockito.spy(DiskManipulationService())

    photosRepository = PhotosRepository(
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
  }

  open fun tearDown() {
  }

  fun createPhoto(
    photoId: Long,
    userId: Long,
    exchangedPhotoId: Long,
    locationMapId: Long,
    photoName: String,
    isPublic: Boolean,
    lon: Double,
    lat: Double,
    uploadedOn: Long,
    deletedOn: Long,
    ipHash: String
  ): Photo {
    return Photo(
      PhotoId(photoId),
      UserId(userId),
      ExchangedPhotoId(exchangedPhotoId),
      LocationMapId(locationMapId),
      PhotoName(photoName),
      isPublic,
      lon,
      lat,
      uploadedOn,
      deletedOn,
      IpHash(ipHash)
    )
  }

}