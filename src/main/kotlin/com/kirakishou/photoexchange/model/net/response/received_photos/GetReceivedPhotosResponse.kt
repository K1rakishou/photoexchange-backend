package com.kirakishou.photoexchange.model.net.response.received_photos

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.StatusResponse

class GetReceivedPhotosResponse
private constructor(

	@SerializedName("received_photos")
	val receivedPhotos: List<ReceivedPhoto>,

	errorCode: ErrorCode.GetReceivedPhotosErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(uploadedPhotos: List<ReceivedPhoto>): GetReceivedPhotosResponse {
			return GetReceivedPhotosResponse(uploadedPhotos, ErrorCode.GetReceivedPhotosErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.GetReceivedPhotosErrors): GetReceivedPhotosResponse {
			return GetReceivedPhotosResponse(emptyList(), errorCode)
		}
	}

	class ReceivedPhoto(

		@SerializedName("photo_id")
		val photoId: Long,

		@SerializedName("uploaded_photo_name")
		val uploadedPhotoName: String,

		@SerializedName("received_photo_name")
		val receivedPhotoName: String,

		@SerializedName("receiver_lon")
		val receiverLon: Double,

		@SerializedName("receiver_lat")
		val receiverLat: Double
	)
}
