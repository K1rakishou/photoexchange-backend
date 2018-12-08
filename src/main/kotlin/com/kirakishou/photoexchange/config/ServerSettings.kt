package com.kirakishou.photoexchange.config

import org.springframework.core.io.ClassPathResource

object ServerSettings {
	//use your mapbox access token here (https://www.mapbox.com/help/how-access-tokens-work/)
	val MAPBOX_ACCESS_TOKEN by lazy { getPropertyByName("MAPBOX_ACCESS_TOKEN") }
	//use your firebase project id here (https://support.google.com/googleapi/answer/7014113?hl=en)
	val PROJECT_ID by lazy { getPropertyByName("FIREBASE_PROJECT_ID") }

	//used for verification that you are really is an admin when you want to do some admin stuff
	//see: handlers/admin
	const val authTokenHeaderName = "X-Auth-Token"

	const val FILE_DIR_PATH = "D:\\projects\\data\\photos"
	const val MAX_PHOTO_SIZE = 10 * (1024 * 1024) //10 megabytes
	//TODO: change in production
	const val OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL = 1000L * 60L * 60L 	//1 hour
//	const val OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL = 1000L * 60L * 5L 	//5 minutes
	//TODO: change in production
	const val DELETE_PHOTOS_OLDER_THAN = 1000L * 60L * 60L * 24L * 30L 	//30 days
//	const val DELETE_PHOTOS_OLDER_THAN = 1000L * 60L * 60L				//60 minutes

	const val PHOTOS_DELIMITER = ','

  const val MIN_GALLERY_PHOTOS_PER_REQUEST_COUNT = 5
	const val MAX_GALLERY_PHOTOS_PER_REQUEST_COUNT = 200
	const val MIN_UPLOADED_PHOTOS_PER_REQUEST_COUNT = 5
	const val MAX_UPLOADED_PHOTOS_PER_REQUEST_COUNT = 200
	const val MIN_RECEIVED_PHOTOS_PER_REQUEST_COUNT = 5
	const val MAX_RECEIVED_PHOTOS_PER_REQUEST_COUNT = 200

  const val VERY_BIG_PHOTO_SIZE = 2048
	const val BIG_PHOTO_SIZE = 1024
	const val MEDIUM_PHOTO_SIZE = 512
	const val SMALL_PHOTO_SIZE = 256
  const val VERY_BIG_PHOTO_SUFFIX = "_vb"
	const val BIG_PHOTO_SUFFIX = "_b"
	const val MEDIUM_PHOTO_SUFFIX = "_m"
	const val SMALL_PHOTO_SUFFIX = "_s"
	const val PHOTO_MAP_SUFFIX = "_map"

	val PHOTO_SIZES = arrayOf("vb", "b", "m", "s")

	object DatabaseInfo {
		const val HOST = "192.168.99.100"
		const val PORT = 27017
		const val DB_NAME = "photoexchange"
	}

	object TestDatabaseInfo {
		const val HOST = "192.168.99.100"
		const val PORT = 27017
		const val DB_NAME = "photoexchange_test"
	}

	private fun getPropertyByName(propertyName: String): String {
		val fileResource = ClassPathResource("keys.properties")

		val keysMap = fileResource.file
			.readLines()
			.map {
				val splitted = it.split("=")
				return@map Pair(splitted[0], splitted[1])
			}
			.toMap()

		return keysMap[propertyName]!!
	}
}