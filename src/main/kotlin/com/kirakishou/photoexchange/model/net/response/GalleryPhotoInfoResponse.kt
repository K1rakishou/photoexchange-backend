package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GalleryPhotoInfoResponse
private constructor(

	@SerializedName("gallery_photos_info")
	val galleryPhotosInfo: List<GalleryPhotosInfoData>,

	errorCode: ErrorCode.GalleryPhotosInfoError
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(galleryPhotosInfo: List<GalleryPhotosInfoData>): GalleryPhotoInfoResponse {
			return GalleryPhotoInfoResponse(galleryPhotosInfo, ErrorCode.GalleryPhotosInfoError.Ok())
		}

		fun fail(errorCode: ErrorCode.GalleryPhotosInfoError): GalleryPhotoInfoResponse {
			return GalleryPhotoInfoResponse(emptyList(), errorCode)
		}
	}

	class GalleryPhotosInfoData(

		@SerializedName("id")
		val id: Long,

		@SerializedName("is_favourited")
        val isFavourited: Boolean,

        @SerializedName("is_reported")
        val isReported: Boolean
	)
}