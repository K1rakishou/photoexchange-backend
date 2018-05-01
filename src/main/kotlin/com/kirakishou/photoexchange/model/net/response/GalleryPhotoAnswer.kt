package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName

class GalleryPhotoAnswer(
	@SerializedName("id")
	val id: Long,

	@SerializedName("photo_name")
	val photoName: String,

	@SerializedName("lon")
	val lon: Double,

	@SerializedName("lat")
	val lat: Double,

	@SerializedName("uploaded_on")
	val uploadedOn: Long,

	@SerializedName("likes_count")
	val likesCount: Long
)