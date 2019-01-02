package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.core.BanId
import com.kirakishou.photoexchange.core.IpHash
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.database.table.Bans
import org.jetbrains.exposed.sql.ResultRow

data class BanEntity(
  val banId: BanId,
  val userId: UserId,
  val ipHash: IpHash,
  val bannedOn: Long
) {

  fun isEmpty() = banId.isEmpty()

  companion object {
    fun create(userId: UserId, ipHash: IpHash, addedOn: Long): BanEntity {
      return BanEntity(
        BanId.empty(),
        userId,
        ipHash,
        addedOn
      )
    }

    fun empty(): BanEntity {
      return BanEntity(
        BanId.empty(),
        UserId.empty(),
        IpHash.empty(),
        -1L
      )
    }

    fun fromResultRow(resultRow: ResultRow): BanEntity {
      return BanEntity(
        BanId(resultRow[Bans.id]),
        UserId(resultRow[Bans.userId]),
        IpHash(resultRow[Bans.ipHash]),
        resultRow[Bans.bannedOn]
      )
    }
  }

}