package com.kirakishou.photoexchange.extensions

import org.springframework.util.MultiValueMap

fun <K, V> MultiValueMap<K, V>.containsAllParts(vararg names: K): Boolean {
	return names.all { this.containsKey(it) }
}