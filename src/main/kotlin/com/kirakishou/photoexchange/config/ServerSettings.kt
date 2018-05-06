package com.kirakishou.photoexchange.config

object ServerSettings {
	const val FILE_DIR_PATH = "D:\\projects\\data\\photos"

	const val MAX_PHOTO_SIZE = 10 * (1024 * 1024) //10 megabytes
	const val OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL = 1000L * 60L * 60L // 1 hour
	const val DELETE_PHOTOS_OLDER_THAN = 1000L * 60L * 60L * 24L * 30L //30 days

	object ThreadPool {
		object Mongo {
			const val MONGO_POOL_NAME = "mongoPool"
			const val MONGO_THREADS_PERCENTAGE = 0.25
		}

		object Common {
			const val COMMON_POOL_NAME = "commonPool"
			const val COMMON_THREADS_PERCENTAGE = 0.75
		}
	}

	object DatabaseInfo {
		const val HOST = "192.168.99.100"
		const val PORT = 27017
		const val DB_NAME = "photoexhange"
	}
}