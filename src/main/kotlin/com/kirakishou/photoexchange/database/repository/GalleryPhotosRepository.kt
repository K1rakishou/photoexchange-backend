package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.ConcurrencyService
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class GalleryPhotosRepository(
	private val photoInfoDao: PhotoInfoDao,
	private val galleryPhotoDao: GalleryPhotoDao,
	private val concurrentService: ConcurrencyService
) {
	private val mutex = Mutex()

	suspend fun findPaged(lastId: Long, count: Int = 25): List<PhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val galleryPhotos = galleryPhotoDao.findPaged(lastId, count)
				val galleryPhotoIds = galleryPhotos.map { it.photoId }

				return@withLock photoInfoDao.findMany(galleryPhotoIds)
			}
		}.await()
	}
}