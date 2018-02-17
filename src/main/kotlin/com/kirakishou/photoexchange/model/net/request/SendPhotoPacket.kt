package com.kirakishou.photoexchange.model.net.request

import com.google.gson.annotations.SerializedName

class SendPhotoPacket(
	@SerializedName("lon")
	val lon: Double,

	@SerializedName("lat")
	val lat: Double,

	@SerializedName("user_id")
	val userId: String
) {
	fun isPacketOk(): Boolean {
		if (lon < -180.0 || lon > 180.0 || lat < -90.0 || lat > 90.0 || userId.isEmpty()) {
			return false
		}

		return true
	}
}