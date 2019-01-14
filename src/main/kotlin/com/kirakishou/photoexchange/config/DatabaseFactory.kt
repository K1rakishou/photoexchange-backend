package com.kirakishou.photoexchange.config

import com.kirakishou.photoexchange.database.table.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction

class DatabaseFactory {
  val db: Database by lazy { Database.connect(hikari()) }

  init {
    transaction(db) {
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

  private fun hikari(): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = "org.postgresql.Driver"
    config.username = ServerSettings.DATABASE_LOGIN
    config.password = ServerSettings.DATABASE_PASSWORD
    config.jdbcUrl = "jdbc:postgresql://192.168.99.100:5432/"
    config.maximumPoolSize = 20
    config.isAutoCommit = false
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    config.validate()
    return HikariDataSource(config)
  }

}