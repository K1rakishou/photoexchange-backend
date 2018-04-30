package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.model.repo.GalleryPhoto
import com.kirakishou.photoexchange.service.ConcurrencyService

class GalleryPhotosRepository(
	private val photoInfoDao: PhotoInfoDao,
	private val galleryPhotoDao: GalleryPhotoDao,
	private val concurrentService: ConcurrencyService
) {

	suspend fun findPaged(lastId: Long, count: Int = 25): List<GalleryPhoto> {
		return concurrentService.asyncMongo {
			return@asyncMongo galleryPhotoDao.findPaged(lastId, count)
		}.await()
	}
}