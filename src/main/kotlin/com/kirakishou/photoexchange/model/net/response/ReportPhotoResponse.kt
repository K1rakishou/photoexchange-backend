package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class ReportPhotoResponse
private constructor(

	@SerializedName("is_reported")
	val isReported: Boolean,

	errorCode: ErrorCode.ReportPhotoErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(isReported: Boolean): ReportPhotoResponse {
			return ReportPhotoResponse(isReported, ErrorCode.ReportPhotoErrors.Ok())
		}

		fun fail(errorCode: ErrorCode.ReportPhotoErrors): ReportPhotoResponse {
			return ReportPhotoResponse(false, errorCode)
		}
	}
}