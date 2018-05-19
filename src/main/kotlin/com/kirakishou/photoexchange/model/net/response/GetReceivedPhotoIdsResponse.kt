package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

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