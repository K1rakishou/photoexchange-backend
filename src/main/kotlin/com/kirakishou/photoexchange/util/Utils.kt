package com.kirakishou.photoexchange.util

import org.slf4j.LoggerFactory

object Utils {
  private val logger = LoggerFactory.getLogger(Utils::class.java)

  fun parsePhotoNames(photoNames: String, maxCount: Int, delimiter: Char): List<String> {
    return try {
      photoNames
        .split(delimiter)
        .asSequence()
        .take(maxCount)
        .filter { it != "" }
        .toList()
    } catch (error: Throwable) {
      logger.error("Unknown error", error)
      return emptyList()
    }
  }
}