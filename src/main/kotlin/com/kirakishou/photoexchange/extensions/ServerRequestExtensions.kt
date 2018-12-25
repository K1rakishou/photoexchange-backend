package com.kirakishou.photoexchange.extensions

import org.springframework.web.reactive.function.server.ServerRequest
import java.lang.NumberFormatException

fun ServerRequest.containsAllParams(vararg names: String): Boolean {
	return names.all { this.queryParams().containsKey(it) }
}

fun ServerRequest.containsAllPathVars(vararg names: String): Boolean {
	return names.all { this.pathVariables().containsKey(it) }
}

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