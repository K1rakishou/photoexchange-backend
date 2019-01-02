package com.kirakishou.photoexchange.database.repository

import com.kirakishou.photoexchange.core.IpHash
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.database.dao.BansDao
import com.kirakishou.photoexchange.database.entity.BanEntity
import com.kirakishou.photoexchange.util.TimeUtils
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

open class BanListRepository(
  private val bansDao: BansDao,
  database: Database,
  dispatcher: CoroutineDispatcher
) : AbstractRepository(database, dispatcher) {
  private val logger = LoggerFactory.getLogger(BanListRepository::class.java)

  open suspend fun ban(userId: UserId, ipHash: IpHash): Boolean {
    return dbQuery(false) {
      val banEntry = BanEntity.create(userId, ipHash, TimeUtils.getTimeFast())
      return@dbQuery bansDao.save(banEntry)
    }
  }

  open suspend fun isBanned(ipHash: IpHash): Boolean {
    return dbQuery(false) {
      return@dbQuery !bansDao.find(ipHash).isEmpty()
    }
  }

  open suspend fun banMany(userIdList: List<UserId>, ipHashList: List<IpHash>): Boolean {
    return dbQuery(false) {
      val time = TimeUtils.getTimeFast()

      if (userIdList.size != ipHashList.size) {
        throw IllegalArgumentException("userIdList.size (${userIdList.size}) != ipHashList.size (${ipHashList.size})")
      }

      val banEntityList = (0 until userIdList.size)
        .map { index -> BanEntity.create(userIdList[index], ipHashList[index], time) }

      return@dbQuery bansDao.saveMany(banEntityList)
    }
  }
}