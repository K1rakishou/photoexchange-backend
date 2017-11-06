package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName

open class StatusResponse(
        @SerializedName("server_error_code")
        var errorCode: Int
)