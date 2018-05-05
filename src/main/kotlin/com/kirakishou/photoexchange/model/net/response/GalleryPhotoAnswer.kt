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

	@SerializedName("favourites_count")
	val favouritesCount: Long,

	@SerializedName("is_favourited")
	val isFavourited: Boolean,

	@SerializedName("is_reported")
	val isReported: Boolean
)