package com.kirakishou.photoexchange.model.net.response

import com.kirakishou.photoexchange.model.ErrorCode

class FavouritePhotoResponse
private constructor(
	errorCode: ErrorCode.FavouritePhotoErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(): FavouritePhotoResponse {
			return FavouritePhotoResponse(ErrorCode.FavouritePhotoErrors.Ok())
		}

		fun fail(errorCode: ErrorCode.FavouritePhotoErrors): FavouritePhotoResponse {
			return FavouritePhotoResponse(errorCode)
		}
	}
}