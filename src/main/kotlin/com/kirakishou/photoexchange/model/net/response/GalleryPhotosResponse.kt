package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GalleryPhotosResponse
private constructor(

	@SerializedName("gallery_photos")
	val galleryPhotos: List<GalleryPhotoAnswer>,

	errorCode: ErrorCode.GalleryPhotosErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(galleryPhotos: List<GalleryPhotoAnswer>): GalleryPhotosResponse {
			return GalleryPhotosResponse(galleryPhotos, ErrorCode.GalleryPhotosErrors.Ok())
		}

		fun fail(errorCode: ErrorCode.GalleryPhotosErrors): GalleryPhotosResponse {
			return GalleryPhotosResponse(emptyList(), errorCode)
		}
	}
}