package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class GetUserIdResponse
private constructor(

	@SerializedName("user_id")
	val userId: String?,

	errorCode: ErrorCode.GetUserIdError
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(userId: String): GetUserIdResponse {
			return GetUserIdResponse(userId, ErrorCode.GetUserIdError.Ok())
		}

		fun fail(errorCode: ErrorCode.GetUserIdError): GetUserIdResponse {
			return GetUserIdResponse(null, errorCode)
		}
	}
}