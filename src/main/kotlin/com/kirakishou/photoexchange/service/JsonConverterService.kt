package com.kirakishou.photoexchange.service

import com.google.gson.Gson
import org.springframework.core.io.buffer.DataBuffer

class JsonConverterService(
	val gson: Gson
) {

	@Suppress("UNCHECKED_CAST")
	inline fun <reified T> fromJson(dataBufferList: List<DataBuffer>, maxLen: Int = 65536): T {
		return gson.fromJson(dataBufferToString(dataBufferList, maxLen), T::class.java) as T
	}

	fun <T> toJson(data: T): String {
		return gson.toJson(data)
	}

	fun dataBufferToString(dataBufferList: List<DataBuffer>, maxLen: Int): String {
		val fullLength = dataBufferList.sumBy { it.readableByteCount() }
		if (fullLength > maxLen) {
			throw PacketSizeExceeded()
		}

		val array = ByteArray(fullLength)
		var offset = 0

		for (dataBuffer in dataBufferList) {
			val arrayLength = dataBuffer.readableByteCount()
			dataBuffer.read(array, offset, arrayLength)
			offset += arrayLength
		}

		return String(array)
	}

	class PacketSizeExceeded() : Exception()
}