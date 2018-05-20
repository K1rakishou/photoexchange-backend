package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class ReceivePhotosResponse
private constructor(

	@Expose
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
		@Expose
		@SerializedName("uploaded_photo_name")
		val uploadedPhotoName: String,

		@Expose
		@SerializedName("received_photo_name")
		val receivedPhotoName: String,

		@Expose
		@SerializedName("lon")
		val lon: Double,

		@Expose
		@SerializedName("lat")
		val lat: Double
	)
}