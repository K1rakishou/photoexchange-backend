package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ServerErrorCode

class PhotoAnswerResponse(

        @SerializedName("owner")
        val owner: String,

        @SerializedName("photo_name")
        val photoName: String,

        errorCode: ServerErrorCode
) : StatusResponse(errorCode.value)