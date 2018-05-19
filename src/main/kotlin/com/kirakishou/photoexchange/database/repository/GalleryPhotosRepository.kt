package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.model.repo.GalleryPhoto
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class GalleryPhotosRepository(
	private val photoInfoDao: PhotoInfoDao,
	private val galleryPhotoDao: GalleryPhotoDao,
	private val concurrentService: ConcurrencyService
) {
	private val mutex = Mutex()

	suspend fun findPaged(lastId: Long, count: Int): List<Long> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				return@withLock galleryPhotoDao.findPaged(lastId, count).map { it.id }
			}
		}.await()
	}

	suspend fun findManyByIdList(photoIds: List<Long>): List<GalleryPhoto> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				return@asyncMongo galleryPhotoDao.findManyByIdList(photoIds)
			}
		}.await()
	}
}