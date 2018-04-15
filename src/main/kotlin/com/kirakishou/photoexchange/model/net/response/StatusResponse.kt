package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

open class StatusResponse(
	@SerializedName("server_error_code")
	var errorCode: Int
) {

	companion object {
		fun from(errorCode: ErrorCode) = StatusResponse(errorCode.value)
	}
}