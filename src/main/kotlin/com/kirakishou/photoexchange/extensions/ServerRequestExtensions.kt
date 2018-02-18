package com.kirakishou.photoexchange.extensions

import org.springframework.web.reactive.function.server.ServerRequest

fun ServerRequest.containsAllParams(vararg names: String): Boolean {
	return names.all { this.queryParams().containsKey(it) }
}

fun ServerRequest.containsAllPathVars(vararg names: String): Boolean {
	return names.all { this.pathVariables().containsKey(it) }
}