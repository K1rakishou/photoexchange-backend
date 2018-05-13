package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GalleryPhotosResponse
private constructor(

	@SerializedName("gallery_photos")
	val galleryPhoto: List<GalleryPhotoResponseData>,

	errorCode: ErrorCode.GalleryPhotosErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(galleryPhotos: List<GalleryPhotoResponseData>): GalleryPhotosResponse {
			return GalleryPhotosResponse(galleryPhotos, ErrorCode.GalleryPhotosErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.GalleryPhotosErrors): GalleryPhotosResponse {
			return GalleryPhotosResponse(emptyList(), errorCode)
		}
	}

    class GalleryPhotoResponseData(

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
        val favouritesCount: Long
    )
}