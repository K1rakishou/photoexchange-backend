package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GetUserIdResponse
private constructor(

	@SerializedName("user_id")
	val userId: String?,

	errorCode: ErrorCode.GetUserIdErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(userId: String): GetUserIdResponse {
			return GetUserIdResponse(userId, ErrorCode.GetUserIdErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.GetUserIdErrors): GetUserIdResponse {
			return GetUserIdResponse(null, errorCode)
		}
	}
}