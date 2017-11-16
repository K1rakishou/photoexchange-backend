package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ServerErrorCode

class UploadPhotoResponse(

    @SerializedName("photo_name")
    val photoName: String?,

    errorCode: ServerErrorCode
) : StatusResponse(errorCode.value) {

    companion object {
        fun success(photoName: String, errorCode: ServerErrorCode): UploadPhotoResponse {
            return UploadPhotoResponse(photoName, errorCode)
        }

        fun fail(errorCode: ServerErrorCode): UploadPhotoResponse {
            return UploadPhotoResponse(null, errorCode)
        }
    }
}