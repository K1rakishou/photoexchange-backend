package com.kirakishou.photoexchange.model.net.response.uploaded_photos

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.StatusResponse

class GetUploadedPhotosResponse
private constructor(

	@SerializedName("uploaded_photos")
	val uploadedPhotos: List<UploadedPhoto>,

	errorCode: ErrorCode.GetUploadedPhotosErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(uploadedPhotos: List<UploadedPhoto>): GetUploadedPhotosResponse {
			return GetUploadedPhotosResponse(uploadedPhotos, ErrorCode.GetUploadedPhotosErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.GetUploadedPhotosErrors): GetUploadedPhotosResponse {
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
		val uploaderLat: Double,

		@SerializedName("has_receiver_info")
		val hasReceivedInfo: Boolean,

		@SerializedName("uploaded_on")
		val uploadedOn: Long
	)
}