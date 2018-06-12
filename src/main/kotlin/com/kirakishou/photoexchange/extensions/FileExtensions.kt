package com.kirakishou.photoexchange.extensions

import java.io.File

fun File.deleteIfExists(): Boolean {
	if (this.exists()) {
		return this.delete()
	}

	return true
}