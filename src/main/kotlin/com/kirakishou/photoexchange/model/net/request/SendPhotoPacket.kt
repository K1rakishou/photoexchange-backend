package com.kirakishou.photoexchange.model.net.request

import com.google.gson.annotations.SerializedName

class SendPhotoPacket(
        @SerializedName("lon")
        val lon: Double,

        @SerializedName("lat")
        val lat: Double,

        @SerializedName("user_id")
        val userId: String
)