package com.kirakishou.photoexchange.extensions

import java.io.File

fun File.deleteIfExists(): Boolean {
	if (exists()) {
		return delete()
	}

	return true
}