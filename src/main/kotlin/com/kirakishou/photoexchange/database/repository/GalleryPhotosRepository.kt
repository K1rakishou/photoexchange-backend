package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

class GalleryPhotosRepository(
	private val photoInfoDao: PhotoInfoDao,
	private val galleryPhotoDao: GalleryPhotoDao
) : AbstractRepository() {
	private val mutex = Mutex()

	suspend fun findPaged(lastId: Long, count: Int): List<Long> {
		return withContext(coroutineContext) {
			return@withContext galleryPhotoDao.findPaged(lastId, count)
				.map { it.map { it.id } }
				.awaitFirst()
		}
	}
}