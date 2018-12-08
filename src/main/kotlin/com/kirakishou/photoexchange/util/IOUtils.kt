package com.kirakishou.photoexchange.util

import org.springframework.core.io.buffer.DataBuffer
import java.io.File

object IOUtils {

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

}