package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.FavouritedPhotoDao
import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.ReportedPhotoDao
import com.kirakishou.photoexchange.model.repo.GalleryPhoto
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.ConcurrencyService
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class GalleryPhotosRepository(
	private val photoInfoDao: PhotoInfoDao,
	private val galleryPhotoDao: GalleryPhotoDao,
	private val favouritedPhotoDao: FavouritedPhotoDao,
	private val reportedPhotoDao: ReportedPhotoDao,
	private val concurrentService: ConcurrencyService
) {
	private val mutex = Mutex()

	suspend fun findPaged(userId: String, lastId: Long, count: Int): LinkedHashMap<Long, GalleryPhotoInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val resultMap = linkedMapOf<Long, GalleryPhotoInfo>()
				val galleryPhotos = galleryPhotoDao.findPaged(lastId, count)
				val galleryPhotoIds = galleryPhotos.map { it.photoId }
				val photoInfos = photoInfoDao.findMany(galleryPhotoIds)

				for (photo in photoInfos) {
					val galleryPhoto = galleryPhotos.first { it.photoId == photo.photoId }
					resultMap[photo.photoId] = GalleryPhotoInfo(photo, galleryPhoto)
				}

				val favouritedPhotoIdsSet = favouritedPhotoDao.findMany(userId, resultMap.keys.toList())
					.map { it.photoId }
					.toSet()

				val reportedPhotosIdsSet = reportedPhotoDao.findMany(userId, resultMap.keys.toList())
					.map { it.photoId }
					.toSet()

				for ((photoId, galleryPhotoInfo) in resultMap) {
					galleryPhotoInfo.isFavourited = favouritedPhotoIdsSet.contains(photoId)
					galleryPhotoInfo.isReported = reportedPhotosIdsSet.contains(photoId)
				}

				return@withLock resultMap
			}
		}.await()
	}

	data class GalleryPhotoInfo(
		val photoInfo: PhotoInfo,
		val galleryPhotoDao: GalleryPhoto,
		var isFavourited: Boolean = false,
		var isReported: Boolean = false
	)
}