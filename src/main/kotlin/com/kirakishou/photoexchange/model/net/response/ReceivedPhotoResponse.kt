package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class ReceivedPhotoResponse
private constructor(

	@SerializedName("received_photos")
	val receivedPhotos: List<ReceivedPhoto>,

	errorCode: ErrorCode.ReceivePhotosErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(receivedPhotoList: List<ReceivedPhoto>): ReceivedPhotoResponse {
			return ReceivedPhotoResponse(receivedPhotoList, ErrorCode.ReceivePhotosErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.ReceivePhotosErrors): ReceivedPhotoResponse {
			return ReceivedPhotoResponse(emptyList(), errorCode)
		}
	}

	class ReceivedPhoto(
		@SerializedName("uploaded_photo_name")
		val uploadedPhotoName: String,

		@SerializedName("received_photo_name")
		val photoAnswerName: String,

		@SerializedName("lon")
		val lon: Double,

		@SerializedName("lat")
		val lat: Double
	)
}