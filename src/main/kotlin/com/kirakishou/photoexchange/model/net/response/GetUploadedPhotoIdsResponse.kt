package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GetUploadedPhotoIdsResponse
private constructor(

	@SerializedName("uploaded_photo_ids")
	val uploadedPhotoIds: List<Long>,

	errorCode: ErrorCode.GetUploadedPhotoIdsError
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(uploadedPhotoIds: List<Long>): GetUploadedPhotoIdsResponse {
			return GetUploadedPhotoIdsResponse(uploadedPhotoIds, ErrorCode.GetUploadedPhotoIdsError.Ok())
		}

		fun fail(errorCode: ErrorCode.GetUploadedPhotoIdsError): GetUploadedPhotoIdsResponse {
			return GetUploadedPhotoIdsResponse(emptyList(), errorCode)
		}
	}
}