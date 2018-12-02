package com.kirakishou.photoexchange.database.entity

import com.kirakishou.photoexchange.database.dao.BanListDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = BanListDao.COLLECTION_NAME)
data class BanEntry(
  @Id
  @Field(Mongo.Field.ID)
  val id: Long,

  @Indexed(name = Mongo.Index.IP_HASH, unique = true)
  @Field(Mongo.Field.IP_HASH)
  val ipHash: String,

  @Field(Mongo.Field.ADDED_ON)
  val addedOn: Long
) {

  fun isEmpty(): Boolean {
    return id == -1L
  }

  companion object {
    fun create(id: Long, ipHash: String, addedOn: Long): BanEntry {
      return BanEntry(id, ipHash, addedOn)
    }

    fun empty(): BanEntry {
      return BanEntry(-1L, "", -1L)
    }
  }

  object Mongo {
    object Field {
      const val ID = "_id"
      const val IP_HASH = "ip_hash"
      const val ADDED_ON = "added_on"
    }

    object Index {
      const val IP_HASH = "ip_hash_index"
    }
  }
}