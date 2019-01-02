package com.kirakishou.photoexchange.config

import com.kirakishou.photoexchange.database.table.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseFactory {
  lateinit var db: Database

  init {
    db = Database.connect(hikariPgsql())

    transaction {
      create(
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

  private fun hikariPgsql(): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = "jdbc:postgresql://192.168.99.100:5432/photoexchange"
    config.username = ServerSettings.DATABASE_LOGIN
    config.password = ServerSettings.DATABASE_PASSWORD
    config.jdbcUrl = "org.postgresql.Driver"
    config.maximumPoolSize = 20
    config.isAutoCommit = false
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    config.validate()
    return HikariDataSource(config)
  }

  //for tests
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