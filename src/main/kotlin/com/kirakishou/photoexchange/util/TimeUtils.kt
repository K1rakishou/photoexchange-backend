package com.kirakishou.photoexchange.util

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

object TimeUtils {
	//default value for datetime database fields
	val dateTimeZero = DateTime(0L)

	@Synchronized
	fun getCurrentDateTime(): DateTime {
		return DateTime.now(DateTimeZone.getDefault())
	}
}