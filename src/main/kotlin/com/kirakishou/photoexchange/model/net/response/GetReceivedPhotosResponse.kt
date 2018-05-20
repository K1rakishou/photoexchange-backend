package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GetReceivedPhotosResponse
private constructor(

	@SerializedName("received_photos")
	val receivedPhotos: List<ReceivedPhoto>,

	errorCode: ErrorCode.GetReceivedPhotosError
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(uploadedPhotos: List<ReceivedPhoto>): GetReceivedPhotosResponse {
			return GetReceivedPhotosResponse(uploadedPhotos, ErrorCode.GetReceivedPhotosError.Ok)
		}

		fun fail(errorCode: ErrorCode.GetReceivedPhotosError): GetReceivedPhotosResponse {
			return GetReceivedPhotosResponse(emptyList(), errorCode)
		}
	}

	class ReceivedPhoto(

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
