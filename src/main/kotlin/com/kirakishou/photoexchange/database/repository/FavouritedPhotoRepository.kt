package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.FavouritedPhotoDao
import com.kirakishou.photoexchange.model.repo.FavouritedPhoto
import com.kirakishou.photoexchange.service.ConcurrencyService
import kotlinx.coroutines.experimental.sync.Mutex

class FavouritedPhotoRepository(
	private val favouritedPhotoDao: FavouritedPhotoDao,
	private val concurrentService: ConcurrencyService
) {
	private val mutex = Mutex()

	suspend fun findMany(userId: String, photoIdList: List<Long>): List<FavouritedPhoto> {
		return concurrentService.asyncMongo {
			return@asyncMongo favouritedPhotoDao.findMany(userId, photoIdList)
		}.await()
	}
}