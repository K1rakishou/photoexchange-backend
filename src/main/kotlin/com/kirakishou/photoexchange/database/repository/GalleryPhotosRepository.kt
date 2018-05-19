package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.sync.Mutex

class GalleryPhotosRepository(
	private val photoInfoDao: PhotoInfoDao,
	private val galleryPhotoDao: GalleryPhotoDao,
	private val concurrentService: ConcurrencyService
) {
	private val mutex = Mutex()

	suspend fun findPaged(lastId: Long, count: Int):List<Long> {
		return galleryPhotoDao.findPaged(lastId, count)
			.map { it.map { it.id } }
			.awaitFirst()
	}
}