package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class UploadPhotoResponse(

	@SerializedName("photo_id")
	val photoId: Long,

	@SerializedName("photo_name")
	val photoName: String,

	errorCode: ErrorCode.UploadPhotoErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(photoId: Long, photoName: String): UploadPhotoResponse {
			return UploadPhotoResponse(photoId, photoName, ErrorCode.UploadPhotoErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.UploadPhotoErrors): UploadPhotoResponse {
			return UploadPhotoResponse(-1L, "", errorCode)
		}
	}
}