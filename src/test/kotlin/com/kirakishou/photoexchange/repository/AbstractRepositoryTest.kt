package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.database.dao.*
import org.springframework.data.mongodb.core.MongoTemplate

abstract class AbstractRepositoryTest {

	lateinit var template: MongoTemplate

	lateinit var mongoSequenceDao: MongoSequenceDao
	lateinit var photoInfoDao: PhotoInfoDao
	lateinit var photoInfoExchangeDao: PhotoInfoExchangeDao
	lateinit var galleryPhotoDao: GalleryPhotoDao
	lateinit var favouritedPhotoDao: FavouritedPhotoDao
	lateinit var reportedPhotoDao: ReportedPhotoDao
	lateinit var userInfoDao: UserInfoDao

	fun init() {
		mongoSequenceDao = MongoSequenceDao(template).also {
			it.clear()
			it.create()
		}
		photoInfoDao = PhotoInfoDao(template).also {
			it.clear()
			it.create()
		}
		photoInfoExchangeDao = PhotoInfoExchangeDao(template).also {
			it.clear()
			it.create()
		}
		galleryPhotoDao = GalleryPhotoDao(template).also {
			it.clear()
			it.create()
		}
		favouritedPhotoDao = FavouritedPhotoDao(template).also {
			it.clear()
			it.create()
		}
		reportedPhotoDao = ReportedPhotoDao(template).also {
			it.clear()
			it.create()
		}
		userInfoDao = UserInfoDao(template).also {
			it.clear()
			it.create()
		}
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