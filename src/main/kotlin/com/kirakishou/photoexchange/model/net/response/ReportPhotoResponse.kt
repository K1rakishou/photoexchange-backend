package com.kirakishou.photoexchange.model.net.response

import com.kirakishou.photoexchange.model.ErrorCode

class ReportPhotoResponse
private constructor(
	errorCode: ErrorCode.ReportPhotoErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(): ReportPhotoResponse {
			return ReportPhotoResponse(ErrorCode.ReportPhotoErrors.Ok())
		}

		fun fail(errorCode: ErrorCode.ReportPhotoErrors): ReportPhotoResponse {
			return ReportPhotoResponse(errorCode)
		}
	}
}