package com.kirakishou.photoexchange.config

import org.springframework.core.io.ClassPathResource

object ServerSettings {
	val GOOGLE_MAPS_KEY by lazy { getGoogleMapsKey() }

	const val FILE_DIR_PATH = "D:\\projects\\data\\photos"
	const val MAX_PHOTO_SIZE = 10 * (1024 * 1024) //10 megabytes
	//TODO: change in production
//	const val OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL = 1000L * 60L * 60L 	//1 hour
	const val OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL = 1000L * 60L * 5L 	//5 minutes
	//TODO: change in production
//	const val DELETE_PHOTOS_OLDER_THAN = 1000L * 60L * 60L * 24L * 30L 	//30 days
	const val DELETE_PHOTOS_OLDER_THAN = 1000L * 60L * 5L				//5 minutes

	const val PHOTOS_DELIMITER = ','

	const val MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT = 100
	const val MAX_UPLOADED_PHOTOS_PER_REQUEST_COUNT = 50
	const val MAX_RECEIVED_PHOTOS_PER_REQUEST_COUNT = 50

	const val BIG_PHOTO_SIZE = 1024
	const val MEDIUM_PHOTO_SIZE = 512
	const val SMALL_PHOTO_SIZE = 256
	const val BIG_PHOTO_SUFFIX = "_b"
	const val MEDIUM_PHOTO_SUFFIX = "_m"
	const val SMALL_PHOTO_SUFFIX = "_s"
	const val PHOTO_MAP_SUFFIX = "_map"

	val PHOTO_SIZES = arrayOf("b", "s", "m")

	object ThreadPool {
		const val MONGO_POOL_NAME = "mongoPool"
		const val COMMON_POOL_NAME = "commonPool"
		const val GOOGLE_MAP_POOL_NAME = "mapPool"
	}

	object DatabaseInfo {
		const val HOST = "192.168.99.100"
		const val PORT = 27017
		const val DB_NAME = "photoexchange"
	}

	private fun getGoogleMapsKey(): String {
		val fileResource = ClassPathResource("keys.properties")
		val googleMapsKeyName = "GOOGLE_MAPS_KEY"

		val keysMap = fileResource.file
			.readLines()
			.map {
				val splitted = it.split("=")
				return@map Pair(splitted[0], splitted[1])
			}
			.toMap()

		return keysMap[googleMapsKeyName]!!
	}
}