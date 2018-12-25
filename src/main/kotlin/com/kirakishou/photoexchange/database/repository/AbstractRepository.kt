package com.kirakishou.photoexchange.database.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class AbstractRepository(
  private val dispatchers: CoroutineDispatcher
) : CoroutineScope {
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchers
}