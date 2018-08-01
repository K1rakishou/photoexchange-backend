package com.kirakishou.photoexchange.util

import com.kirakishou.photoexchange.config.ServerSettings
import org.slf4j.LoggerFactory
import org.springframework.core.io.buffer.DataBuffer
import java.io.File

object IOUtils {
	private val logger = LoggerFactory.getLogger(IOUtils::class.java)

	fun copyDataBuffersToFile(bufferList: List<DataBuffer>, outFile: File) {
		outFile.outputStream().use { outputStream ->
			for (chunk in bufferList) {
				chunk.asInputStream().use { inputStream ->
					val chunkSize = inputStream.available()
					val buffer = ByteArray(chunkSize)

					//copy chunks from one stream to another
					inputStream.read(buffer, 0, chunkSize)
					outputStream.write(buffer, 0, chunkSize)
				}
			}
		}
	}

	fun deleteAllPhotoFiles(photoFullPath: String) {
		val smallPhotoFile = File("$photoFullPath${ServerSettings.SMALL_PHOTO_SUFFIX}")
		if (smallPhotoFile.exists()) {
			if (!smallPhotoFile.delete()) {
				logger.warn("Could not delete file with path: ${smallPhotoFile.absolutePath}")
			}
		}

		val mediumPhotoFile = File("$photoFullPath${ServerSettings.MEDIUM_PHOTO_SUFFIX}")
		if (mediumPhotoFile.exists()) {
			if (!mediumPhotoFile.delete()) {
				logger.warn("Could not delete file with path: ${mediumPhotoFile.absolutePath}")
			}
		}

		val bigPhotoFile = File("$photoFullPath${ServerSettings.BIG_PHOTO_SUFFIX}")
		if (bigPhotoFile.exists()) {
			if (!bigPhotoFile.delete()) {
				logger.warn("Could not delete file with path: ${bigPhotoFile.absolutePath}")
			}
		}

		val mapPhotoFile = File("$photoFullPath${ServerSettings.PHOTO_MAP_SUFFIX}")
		if (mapPhotoFile.exists()) {
			if (!mapPhotoFile.delete()) {
				logger.warn("Could not delete file with path: ${mapPhotoFile.absolutePath}")
			}
		}
	}
}