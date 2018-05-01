package com.kirakishou.photoexchange.model.net.request

import com.google.gson.annotations.SerializedName

class FavouritePhotoPacket(
	@SerializedName("user_id")
	val userId: String,

	@SerializedName("photo_name")
	val photoName: String
) {
	fun isPacketOk(): Boolean {
		return userId != null && photoName != null
	}
}