package com.kirakishou.photoexchange.service

import com.google.gson.Gson
import com.kirakishou.photoexchange.util.Utils
import org.springframework.core.io.buffer.DataBuffer

class JsonConverterService(
	val gson: Gson
) {

	@Suppress("UNCHECKED_CAST")
	inline fun <reified T> fromJson(dataBufferList: List<DataBuffer>): T {
		return gson.fromJson(Utils.dataBufferToString(dataBufferList), T::class.java) as T
	}

	fun <T> toJson(data: T): String {
		return gson.toJson(data)
	}
}