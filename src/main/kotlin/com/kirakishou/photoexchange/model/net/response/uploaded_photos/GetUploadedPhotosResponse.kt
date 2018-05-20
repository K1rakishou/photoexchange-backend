package com.kirakishou.photoexchange.model.net.response.uploaded_photos

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.StatusResponse

class GetUploadedPhotosResponse
private constructor(

	@SerializedName("uploaded_photos")
	val uploadedPhotos: List<UploadedPhoto>,

	errorCode: ErrorCode.GetUploadedPhotosError
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(uploadedPhotos: List<UploadedPhoto>): GetUploadedPhotosResponse {
			return GetUploadedPhotosResponse(uploadedPhotos, ErrorCode.GetUploadedPhotosError.Ok)
		}

		fun fail(errorCode: ErrorCode.GetUploadedPhotosError): GetUploadedPhotosResponse {
			return GetUploadedPhotosResponse(emptyList(), errorCode)
		}
	}

	class UploadedPhoto(

		@SerializedName("photo_id")
		val photoId: Long,

		@SerializedName("photo_name")
		val photoName: String,

		@SerializedName("uploader_lon")
		val uploaderLon: Double,

		@SerializedName("uploader_lat")
		val uploaderLat: Double
	)
}