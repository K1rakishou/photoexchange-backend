package com.kirakishou.photoexchange.util

import com.kirakishou.photoexchange.core.PhotoName
import org.slf4j.LoggerFactory

object Utils {
  private val logger = LoggerFactory.getLogger(Utils::class.java)

  fun parsePhotoNames(photoNames: String, maxCount: Int, delimiter: Char): List<PhotoName>? {
    return try {
      photoNames
        .split(delimiter)
        .asSequence()
        .take(maxCount)
        .filter { it != "" }
        .map { PhotoName(it) }
        .toList()

    } catch (error: Throwable) {
      logger.error("Unknown error", error)
      return null
    }
  }
}