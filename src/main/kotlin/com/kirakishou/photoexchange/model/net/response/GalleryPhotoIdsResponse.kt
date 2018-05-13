package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GalleryPhotoIdsResponse
private constructor(

	@SerializedName("gallery_photo_ids")
	val galleryPhotoIds: List<Long>,

	errorCode: ErrorCode.GalleryPhotosErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(galleryPhotoIds: List<Long>): GalleryPhotoIdsResponse {
			return GalleryPhotoIdsResponse(galleryPhotoIds, ErrorCode.GalleryPhotosErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.GalleryPhotosErrors): GalleryPhotoIdsResponse {
			return GalleryPhotoIdsResponse(emptyList(), errorCode)
		}
	}
}