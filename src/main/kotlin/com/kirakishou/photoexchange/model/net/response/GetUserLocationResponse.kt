package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ServerErrorCode

class GetUserLocationResponse
private constructor(
    @SerializedName("lat")
    val lat: Double?,

    @SerializedName("lon")
    val lon: Double?,

    errorCode: ServerErrorCode
) : StatusResponse(errorCode.value) {

    companion object {
        fun success(lat: Double, lon: Double): GetUserLocationResponse {
            return GetUserLocationResponse(lat, lon, ServerErrorCode.OK)
        }

        fun fail(errorCode: ServerErrorCode): GetUserLocationResponse {
            return GetUserLocationResponse(0.0, 0.0, errorCode)
        }
    }
}