package com.kirakishou.photoexchange.util

import org.slf4j.LoggerFactory

object Utils {
	private val logger = LoggerFactory.getLogger(Utils::class.java)

	fun parsePhotoIds(photoIdsString: String, maxCount: Int, delimiter: Char): List<Long> {
		return try {
			photoIdsString
				.split(delimiter)
				.asSequence()
				.take(maxCount)
				.map { photoId ->
					return@map try {
						photoId.toLong()
					} catch (error: NumberFormatException) {
						-1L
					}
				}
				.filter { it != -1L }
				.toList()
		} catch (error: Throwable) {
			logger.error("Unknown error", error)
			return emptyList()
		}
	}
}