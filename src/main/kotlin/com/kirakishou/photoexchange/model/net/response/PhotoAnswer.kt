package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName

class PhotoAnswer(
	@SerializedName("uploaded_photo_name")
	val uploadedPhotoName: String,

	@SerializedName("photo_answer_name")
	val photoAnswerName: String,

	@SerializedName("lon")
	val lon: Double,

	@SerializedName("lat")
	val lat: Double
)