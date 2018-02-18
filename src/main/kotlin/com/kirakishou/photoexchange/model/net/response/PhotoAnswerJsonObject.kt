package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName

class PhotoAnswerJsonObject(
	@SerializedName("user_id")
	val userId: String,

	@SerializedName("photo_name")
	val photoName: String,

	@SerializedName("lon")
	val lon: Double,

	@SerializedName("lat")
	val lat: Double
)