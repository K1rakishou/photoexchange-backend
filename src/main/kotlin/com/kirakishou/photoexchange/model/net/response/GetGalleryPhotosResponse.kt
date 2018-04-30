package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GetGalleryPhotosResponse
private constructor(

	@SerializedName("gallery_photos")
	val galleryPhotos: List<GalleryPhotoAnswer>,

	errorCode: ErrorCode.GetGalleryPhotosErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(galleryPhotos: List<GalleryPhotoAnswer>): GetGalleryPhotosResponse {
			return GetGalleryPhotosResponse(galleryPhotos, ErrorCode.GetGalleryPhotosErrors.Ok())
		}

		fun fail(errorCode: ErrorCode.GetGalleryPhotosErrors): GetGalleryPhotosResponse {
			return GetGalleryPhotosResponse(emptyList(), errorCode)
		}
	}
}