package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ServerErrorCode

class PhotoAnswerResponse
private constructor(

    @SerializedName("photo_answer_list")
    val photoAnswer: PhotoAnswerJsonObject?,

    @SerializedName("all_found")
    val allFound: Boolean?,

    errorCode: ServerErrorCode
) : StatusResponse(errorCode.value) {

    companion object {
        fun success(photoAnswer: PhotoAnswerJsonObject, allFound: Boolean, errorCode: ServerErrorCode): PhotoAnswerResponse {
            return PhotoAnswerResponse(photoAnswer, allFound, errorCode)
        }

        fun fail(errorCode: ServerErrorCode): PhotoAnswerResponse {
            return PhotoAnswerResponse(null, null, errorCode)
        }
    }
}