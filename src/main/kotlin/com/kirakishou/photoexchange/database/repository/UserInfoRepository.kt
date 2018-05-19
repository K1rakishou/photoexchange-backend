package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.UserInfoDao
import com.kirakishou.photoexchange.model.repo.UserInfo
import com.kirakishou.photoexchange.service.GeneratorService
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import kotlinx.coroutines.experimental.reactive.awaitFirst
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

class UserInfoRepository(
	private val mongoSequenceDao: MongoSequenceDao,
	private val userInfoDao: UserInfoDao,
	private val generator: GeneratorService,
	private val concurrentService: AbstractConcurrencyService
) {
	private val mutex = Mutex()

	private suspend fun generateUserId(): String {
		var userId = ""

		while (true) {
			val generatedUserId = generator.generateUserId()
			if (!userInfoDao.userIdExists(generatedUserId).awaitFirst()) {
				userId = generatedUserId
				break
			}
		}

		return userId
	}

	suspend fun createNew(): UserInfo {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				val userInfo = UserInfo.empty()
				userInfo.userId = generateUserId()
				userInfo.id = mongoSequenceDao.getNextUserId().awaitFirst()

				return@withLock userInfoDao.save(userInfo).awaitFirst()
			}
		}.await()
	}

	suspend fun findManyNotRegistered(userIdList: List<String>): List<UserInfo> {
		return concurrentService.asyncMongo {
			return@asyncMongo mutex.withLock {
				return@withLock userInfoDao.findManyNotRegistered(userIdList).awaitFirst()
			}
		}.await()
	}
}