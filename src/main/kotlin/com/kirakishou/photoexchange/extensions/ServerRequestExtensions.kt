package com.kirakishou.photoexchange.extensions

import org.joda.time.DateTime
import org.springframework.web.reactive.function.server.ServerRequest

fun ServerRequest.getStringVariable(name: String, maxLen: Int): String? {
	if (!pathVariables().containsKey(name)) {
		return null
	}

	val variable = pathVariable(name)
	if (variable.isEmpty()) {
		return null
	}

	if (variable.length > maxLen) {
		return null
	}

	return variable
}

fun ServerRequest.getIntVariable(name: String, min: Int, max: Int): Int? {
	val variableStr = getStringVariable(name, 16)
	if (variableStr == null) {
		return null
	}

	return try {
		variableStr.toInt().coerceIn(min, max)
	} catch (error: NumberFormatException) {
		return null
	}
}

fun ServerRequest.getLongVariable(name: String, min: Long, max: Long): Long? {
	val variableStr = getStringVariable(name, 24)
	if (variableStr == null) {
		return null
	}

	return try {
		variableStr.toLong().coerceIn(min, max)
	} catch (error: NumberFormatException) {
		return null
	}
}

fun ServerRequest.getDoubleValue(name: String, min: Double, max: Double): Double? {
	val variableStr = getStringVariable(name, 24)
	if (variableStr == null) {
		return null
	}

	return try {
		variableStr.toDouble().coerceIn(min, max)
	} catch (error: NumberFormatException) {
		return null
	}
}

fun ServerRequest.getDateTimeVariable(name: String): DateTime? {
	val timeLong = getLongVariable(name, 0, Long.MAX_VALUE)
	if (timeLong == null) {
		return null
	}

	return if (timeLong <= 0L) {
		DateTime.now()
	} else {
		DateTime(timeLong)
	}
}