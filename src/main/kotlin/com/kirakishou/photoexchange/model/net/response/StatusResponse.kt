package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ServerErrorCode

open class StatusResponse(
	@SerializedName("server_error_code")
	var errorCode: Int
) {

	companion object {
		fun from(errorCode: ServerErrorCode) = StatusResponse(errorCode.value)
	}
}