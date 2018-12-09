package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.database.dao.BanListDao
import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.entity.BanEntry
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

open class BanListRepository(
  private val mongoSequenceDao: MongoSequenceDao,
  private val banListDao: BanListDao
) : AbstractRepository() {
  private val mutex = Mutex()
  private val logger = LoggerFactory.getLogger(BanListRepository::class.java)

  open suspend fun ban(ipHash: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val id = mongoSequenceDao.getNextBanEntryId().awaitFirst()
        val banEntry = BanEntry.create(id, ipHash, TimeUtils.getTimeFast())

        return@withLock banListDao.save(banEntry).awaitFirst()
      }
    }
  }

  open suspend fun isBanned(ipHash: String): Boolean {
    return withContext(coroutineContext) {
      return@withContext !banListDao.find(ipHash).awaitFirst().isEmpty()
    }
  }

  open suspend fun banMany(ipHashList: List<String>): Boolean {
    return withContext(coroutineContext) {
      return@withContext mutex.withLock {
        val banEntryList = ipHashList.map { ipHash ->
          val id = mongoSequenceDao.getNextBanEntryId().awaitFirst()
          return@map BanEntry.create(id, ipHash, TimeUtils.getTimeFast())
        }

        return@withLock banListDao.saveMany(banEntryList).awaitFirst()
      }
    }
  }
}