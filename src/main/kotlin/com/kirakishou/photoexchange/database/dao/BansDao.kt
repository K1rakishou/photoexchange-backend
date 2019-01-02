package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.core.IpHash
import com.kirakishou.photoexchange.database.entity.BanEntity
import com.kirakishou.photoexchange.database.table.Bans
import com.kirakishou.photoexchange.util.TimeUtils
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

open class BansDao {

  open fun save(banEntity: BanEntity): Boolean {
    val currentTime = TimeUtils.getTimeFast()

    val id = Bans.insert {
      it[Bans.userId] = banEntity.userId.id
      it[Bans.ipHash] = banEntity.ipHash.hash
      it[Bans.bannedOn] = currentTime
    } get Bans.id

    return id != null
  }

  fun saveMany(banEntityList: List<BanEntity>): Boolean {
    val ids = Bans.batchInsert(banEntityList) { banEntity ->
      val currentTime = TimeUtils.getTimeFast()

      this[Bans.userId] = banEntity.userId.id
      this[Bans.ipHash] = banEntity.ipHash.hash
      this[Bans.bannedOn] = currentTime
    }

    return ids.size == banEntityList.size
  }

  fun find(ipHash: IpHash): BanEntity {
    return Bans.select {
      Bans.ipHash.eq(ipHash.hash)
    }
      .firstOrNull()
      ?.let { resultRow -> BanEntity.fromResultRow(resultRow) }
      ?: BanEntity.empty()
  }
}