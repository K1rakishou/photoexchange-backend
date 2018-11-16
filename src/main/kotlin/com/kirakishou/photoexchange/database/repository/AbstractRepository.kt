package com.kirakishou.photoexchange.database.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

abstract class AbstractRepository : CoroutineScope {
  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.IO
}