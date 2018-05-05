package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GalleryPhotoIdsResponse
private constructor(

	@Expose
	@SerializedName("gallery_photo_ids")
	val galleryPhotoIds: List<Long>,

	errorCode: ErrorCode.GalleryPhotoIdsErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(galleryPhotoIds: List<Long>): GalleryPhotoIdsResponse {
			return GalleryPhotoIdsResponse(galleryPhotoIds, ErrorCode.GalleryPhotoIdsErrors.Ok())
		}

		fun fail(errorCode: ErrorCode.GalleryPhotoIdsErrors): GalleryPhotoIdsResponse {
			return GalleryPhotoIdsResponse(emptyList(), errorCode)
		}
	}
}