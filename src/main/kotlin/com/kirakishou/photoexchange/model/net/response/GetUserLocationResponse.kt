package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ServerErrorCode

class GetUserLocationResponse
private constructor(
    @SerializedName("locations")
    val locationList: List<UserNewLocation>,

    errorCode: ServerErrorCode
) : StatusResponse(errorCode.value) {

    companion object {
        fun success(locations: List<UserNewLocation>): GetUserLocationResponse {
            return GetUserLocationResponse(locations, ServerErrorCode.OK)
        }

        fun fail(errorCode: ServerErrorCode): GetUserLocationResponse {
            return GetUserLocationResponse(emptyList(), errorCode)
        }
    }

    class UserNewLocation(
        @SerializedName("photo_name")
        val photoName: String,

        @SerializedName("lat")
        val lat: Double,

        @SerializedName("lon")
        val lon: Double
    )
}