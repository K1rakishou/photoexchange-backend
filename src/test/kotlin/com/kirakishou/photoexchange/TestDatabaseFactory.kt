package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.database.table.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

class TestDatabaseFactory {
  lateinit var db: Database

  init {
    db = Database.connect(hikariH2())

    transaction {
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

  private fun hikariH2(): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = "org.h2.Driver"
    config.jdbcUrl = "jdbc:h2:mem:test"
    config.maximumPoolSize = 20
    config.isAutoCommit = false
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    config.validate()
    return HikariDataSource(config)
  }

}