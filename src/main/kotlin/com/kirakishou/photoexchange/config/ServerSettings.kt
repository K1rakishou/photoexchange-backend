package com.kirakishou.photoexchange.config

object ServerSettings {
	const val FILE_DIR_PATH = "D:\\projects\\data\\photos"
	const val MONGO_POOL_NAME = "mongo"
	const val MONGO_POOL_BEAN_NAME = "mongoPool"
	const val MAX_PHOTO_SIZE = 10 * (1024 * 1024) //10 megabytes
	const val OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL = 1000L * 60L * 60L // 1 hour
	const val DELETE_PHOTOS_OLDER_THAN = 1000L * 60L * 60L * 24L * 7L //7 days

	object DatabaseInfo {
		const val HOST = "192.168.99.100"
		const val PORT = 27017
		const val DB_NAME = "photoexhange"
	}
}