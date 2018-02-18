package com.kirakishou.photoexchange.extensions

import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.server.ServerRequest

fun ServerRequest.containsAllParts(vararg names: String): Boolean {
	return names.all { this.queryParams().containsKey(it) }
}
