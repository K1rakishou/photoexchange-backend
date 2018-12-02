package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.BanListDao
import com.kirakishou.photoexchange.database.entity.BanEntry
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

open class BanListRepository(
  private val banListDao: BanListDao
) : AbstractRepository() {
  private val mutex = Mutex()
  private val logger = LoggerFactory.getLogger(BanListRepository::class.java)

  open suspend fun ban(ipHash: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock banListDao.save(BanEntry.create(ipHash, TimeUtils.getTimeFast())).awaitFirst()
      }
    }
  }

  open suspend fun isBanned(ipHash: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        return@withLock banListDao.contains(ipHash).awaitFirst()
      }
    }
  }
}