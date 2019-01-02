package com.kirakishou.photoexchange.database.repository

import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

abstract class AbstractRepository(
  private val database: Database,
  private val dispatchers: CoroutineDispatcher
) : CoroutineScope {
  private val logger = LoggerFactory.getLogger(AbstractRepository::class.java)
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchers

  suspend fun <T> dbQuery(defaultOnError: T? = null, block: suspend () -> T): T {
    return withContext(dispatchers) {
      try {
        transaction(database) {
          runBlocking {
            block()
          }
        }
      } catch (error: Throwable) {
        if (defaultOnError == null) {
          throw error
        }

        logger.error("DB error", error)
        return@withContext defaultOnError!!
      }
    }
  }
}