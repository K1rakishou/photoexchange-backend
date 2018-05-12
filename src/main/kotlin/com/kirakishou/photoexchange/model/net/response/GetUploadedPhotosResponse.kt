package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GetUploadedPhotosResponse
private constructor(

	@SerializedName("uploaded_photos")
	val uploadedPhotos: List<UploadedPhoto>,

	errorCode: ErrorCode.GetUploadedPhotosError
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(receivedPhotoList: List<UploadedPhoto>): GetUploadedPhotosResponse {
			return GetUploadedPhotosResponse(receivedPhotoList, ErrorCode.GetUploadedPhotosError.Ok())
		}

		fun fail(errorCode: ErrorCode.GetUploadedPhotosError): GetUploadedPhotosResponse {
			return GetUploadedPhotosResponse(emptyList(), errorCode)
		}
	}

	class UploadedPhoto(

		@SerializedName("photo_id")
		val photoId: Long,

		@SerializedName("photo_name")
		val photoName: String
	)
}