package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class CheckAccountExistsResponse
private constructor(

	@SerializedName("account_exists")
	val accountExists: Boolean,

	errorCode: ErrorCode.CheckAccountExistsErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(accountExists: Boolean): CheckAccountExistsResponse {
			return CheckAccountExistsResponse(accountExists, ErrorCode.CheckAccountExistsErrors.Ok)
		}

		fun fail(errorCode: ErrorCode.CheckAccountExistsErrors): CheckAccountExistsResponse {
			return CheckAccountExistsResponse(false, errorCode)
		}
	}
}