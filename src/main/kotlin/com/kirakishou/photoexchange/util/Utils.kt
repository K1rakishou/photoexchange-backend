package com.kirakishou.photoexchange.util

object Utils {
	fun parseGalleryPhotoIds(photoIdsString: String, delimiter: Char): List<Long> {
		return photoIdsString
			.split(delimiter)
			.map { photoId ->
				return@map try {
					photoId.toLong()
				} catch (error: NumberFormatException) {
					-1L
				}
			}
			.filter { it != -1L }
	}
}