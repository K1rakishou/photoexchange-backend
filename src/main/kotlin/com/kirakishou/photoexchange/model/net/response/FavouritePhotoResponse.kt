package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class FavouritePhotoResponse
private constructor(

	@SerializedName("is_favourited")
	val isFavourited: Boolean,

	@SerializedName("count")
	val favouritesCount: Long,

	errorCode: ErrorCode.FavouritePhotoErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(isFavourited: Boolean, count: Long): FavouritePhotoResponse {
			return FavouritePhotoResponse(isFavourited, count, ErrorCode.FavouritePhotoErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.FavouritePhotoErrors): FavouritePhotoResponse {
			return FavouritePhotoResponse(false, 0, errorCode)
		}
	}
}