package com.kirakishou.photoexchange.model.net.response.uploaded_photos

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode
import com.kirakishou.photoexchange.model.net.response.StatusResponse

class GetUploadedPhotoIdsResponse
private constructor(

	@SerializedName("uploaded_photo_ids")
	val uploadedPhotoIds: List<Long>,

	errorCode: ErrorCode.GetUploadedPhotosError
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(uploadedPhotoIds: List<Long>): GetUploadedPhotoIdsResponse {
			return GetUploadedPhotoIdsResponse(uploadedPhotoIds, ErrorCode.GetUploadedPhotosError.Ok)
		}

		fun fail(errorCode: ErrorCode.GetUploadedPhotosError): GetUploadedPhotoIdsResponse {
			return GetUploadedPhotoIdsResponse(emptyList(), errorCode)
		}
	}
}