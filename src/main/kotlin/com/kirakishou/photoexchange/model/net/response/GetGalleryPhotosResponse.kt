package com.kirakishou.photoexchange.model.net.response

import com.kirakishou.photoexchange.model.ErrorCode

class GetGalleryPhotosResponse
private constructor(

	errorCode: ErrorCode.GetGalleryPhotosErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(): GetGalleryPhotosResponse {
			return GetGalleryPhotosResponse(ErrorCode.GetGalleryPhotosErrors.Ok())
		}

		fun fail(errorCode: ErrorCode.GetGalleryPhotosErrors): GetGalleryPhotosResponse {
			return GetGalleryPhotosResponse(errorCode)
		}
	}
}