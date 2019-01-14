package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.core.BanId
import com.kirakishou.photoexchange.core.IpHash
import com.kirakishou.photoexchange.core.UserId
import com.kirakishou.photoexchange.database.table.Bans
import com.kirakishou.photoexchange.util.TimeUtils
import org.jetbrains.exposed.sql.ResultRow
import org.joda.time.DateTime

data class BanEntity(
  val banId: BanId,
  val userId: UserId,
  val ipHash: IpHash,
  val bannedOn: DateTime
) {

  fun isEmpty() = banId.isEmpty()

  companion object {
    fun create(userId: UserId, ipHash: IpHash, addedOn: DateTime): BanEntity {
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
        TimeUtils.dateTimeZero
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