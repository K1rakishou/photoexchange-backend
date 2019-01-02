package com.kirakishou.photoexchange.config

import com.kirakishou.photoexchange.database.table.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.create
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

  fun init() {
    Database.connect(hikariPgsql())

    transaction {
      create(Users)
      create(Photos)
      create(Bans)
      create(FavouritedPhotos)
      create(ReportedPhotos)
      create(GalleryPhotos)
      create(LocationMaps)
    }
  }

  private fun hikariPgsql(): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = "jdbc:postgresql://192.168.99.100:5432/photoexchange"
    config.username = "postgres"
    config.password = "password"
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