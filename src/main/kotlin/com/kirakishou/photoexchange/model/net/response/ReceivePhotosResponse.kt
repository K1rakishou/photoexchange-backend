package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class ReceivePhotosResponse
private constructor(

	@SerializedName("received_photos")
	val receivedPhotos: List<ReceivedPhoto>,

	errorCode: ErrorCode.ReceivePhotosErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(receivedPhotos: List<ReceivedPhoto>): ReceivePhotosResponse {
			return ReceivePhotosResponse(receivedPhotos, ErrorCode.ReceivePhotosErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.ReceivePhotosErrors): ReceivePhotosResponse {
			return ReceivePhotosResponse(emptyList(), errorCode)
		}
	}

	class ReceivedPhoto(

		@SerializedName("photo_id")
		val photoId: Long,

		@SerializedName("uploaded_photo_name")
		val uploadedPhotoName: String,

		@SerializedName("received_photo_name")
		val receivedPhotoName: String,

		@SerializedName("lon")
		val lon: Double,

		@SerializedName("lat")
		val lat: Double
	)
}