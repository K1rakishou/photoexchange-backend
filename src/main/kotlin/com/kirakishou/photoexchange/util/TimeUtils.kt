package com.kirakishou.photoexchange.util

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

object TimeUtils {

	@Synchronized
	fun getTimeFast(): Long {
		return System.currentTimeMillis()
	}

	@Synchronized
	fun getCurrentDateTime(): DateTime {
		return DateTime.now(DateTimeZone.getDefault())
	}
}