package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.model.repo.GalleryPhoto
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

	suspend fun findPaged(lastId: Long, count: Int): LinkedHashMap<Long, Pair<PhotoInfo, GalleryPhoto>> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val resultMap = linkedMapOf<Long, Pair<PhotoInfo, GalleryPhoto>>()
				val galleryPhotos = galleryPhotoDao.findPaged(lastId, count)
				val galleryPhotoIds = galleryPhotos.map { it.photoId }
				val photoInfos = photoInfoDao.findMany(galleryPhotoIds)

				for (photo in photoInfos) {
					val galleryPhoto = galleryPhotos.first { it.photoId == photo.photoId }
					resultMap[photo.photoId] = Pair(photo, galleryPhoto)
				}

				return@withLock resultMap
			}
		}.await()
	}
}