package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.ReportedPhotoDao
import com.kirakishou.photoexchange.model.repo.ReportedPhoto
import com.kirakishou.photoexchange.service.ConcurrencyService
import kotlinx.coroutines.experimental.sync.Mutex

class ReportedPhotoRepository(
	private val reportedPhotoDao: ReportedPhotoDao,
	private val concurrentService: ConcurrencyService
) {
	private val mutex = Mutex()

	suspend fun findMany(userId: String, photoIdList: List<Long>): List<ReportedPhoto> {
		return concurrentService.asyncMongo {
			return@asyncMongo reportedPhotoDao.findMany(userId, photoIdList)
		}.await()
	}
}