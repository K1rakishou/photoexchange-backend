package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.database.dao.BanListDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = BanListDao.COLLECTION_NAME)
data class BanEntry(
  @Id
  @Field(Mongo.Field.IP_HASH)
  var ipHash: String,

  @Field(Mongo.Field.ADDED_ON)
  var addedOn: Long
) {

  fun isEmpty(): Boolean {
    return ipHash.isEmpty()
  }

  companion object {
    fun create(ipHash: String, addedOn: Long): BanEntry {
      return BanEntry(ipHash, addedOn)
    }

    fun empty(): BanEntry {
      return BanEntry("", -1L)
    }
  }

  object Mongo {
    object Field {
      const val IP_HASH = "ip_hash"
      const val ADDED_ON = "added_on"
    }
  }
}