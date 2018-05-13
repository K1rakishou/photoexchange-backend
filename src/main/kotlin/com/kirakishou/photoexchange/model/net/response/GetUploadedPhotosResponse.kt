package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GetUploadedPhotosResponse
private constructor(

	@SerializedName("uploaded_photos")
	val uploadedPhotoss: List<UploadedPhoto>,

	errorCode: ErrorCode.GetUploadedPhotosError
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(uploadedPhotos: List<UploadedPhoto>): GetUploadedPhotosResponse {
			return GetUploadedPhotosResponse(uploadedPhotos, ErrorCode.GetUploadedPhotosError.Ok())
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

		@SerializedName("receiver_lon")
		val receiverLon: Double,

		@SerializedName("receiver_lat")
		val receiverLat: Double
	)
}