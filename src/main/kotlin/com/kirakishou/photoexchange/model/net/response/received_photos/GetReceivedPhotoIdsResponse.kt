package com.kirakishou.photoexchange.model.net.response.received_photos

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.StatusResponse

class GetReceivedPhotoIdsResponse
private constructor(

	@SerializedName("received_photo_ids")
	val receivedPhotoIds: List<Long>,

	errorCode: ErrorCode.GetReceivedPhotosError
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(uploadedPhotoIds: List<Long>): GetReceivedPhotoIdsResponse {
			return GetReceivedPhotoIdsResponse(uploadedPhotoIds, ErrorCode.GetReceivedPhotosError.Ok)
		}

		fun fail(errorCode: ErrorCode.GetReceivedPhotosError): GetReceivedPhotoIdsResponse {
			return GetReceivedPhotoIdsResponse(emptyList(), errorCode)
		}
	}
}