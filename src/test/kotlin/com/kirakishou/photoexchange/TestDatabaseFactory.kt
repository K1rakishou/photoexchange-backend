package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.database.table.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class TestDatabaseFactory {
  val db: Database by lazy { Database.connect(hikariH2()) }

  fun createTables() {
    transaction(db) {
      SchemaUtils.create(
        Users,
        Photos,
        Bans,
        FavouritedPhotos,
        ReportedPhotos,
        GalleryPhotos,
        LocationMaps
      )
    }
  }

  fun dropTables() {
    transaction(db) {
      SchemaUtils.drop(
        Users,
        Photos,
        Bans,
        FavouritedPhotos,
        ReportedPhotos,
        GalleryPhotos,
        LocationMaps
      )
    }
  }

  private fun hikariH2(): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = "org.h2.Driver"
    config.jdbcUrl = "jdbc:h2:mem:test"
    config.maximumPoolSize = 4
    config.isAutoCommit = false
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    return HikariDataSource(config)
  }

}