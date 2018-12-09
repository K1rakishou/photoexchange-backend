package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.UserInfoDao
import com.kirakishou.photoexchange.database.entity.UserInfo
import com.kirakishou.photoexchange.service.GeneratorService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

open class UserInfoRepository(
  private val mongoSequenceDao: MongoSequenceDao,
  private val userInfoDao: UserInfoDao,
  private val generator: GeneratorService
) : AbstractRepository() {
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
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val userInfo = UserInfo.empty()
        userInfo.userId = generateUserId()
        userInfo.id = mongoSequenceDao.getNextUserId().awaitFirst()

        return@withLock userInfoDao.save(userInfo).awaitFirst()
      }
    }
  }

  suspend fun accountExists(userId: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext userInfoDao.userIdExists(userId).awaitFirst()
    }
  }

  open suspend fun getFirebaseToken(userId: String): String {
    return withContext(coroutineContext) {
      return@withContext userInfoDao.getUser(userId).awaitFirst().firebaseToken
    }
  }

  suspend fun updateFirebaseToken(userId: String, newToken: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock userInfoDao.updateFirebaseToken(userId, newToken).awaitFirst()
      }
    }
  }
}