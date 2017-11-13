package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ServerErrorCode

class PhotoAnswerResponse
private constructor(

    @SerializedName("photo_answer_list")
    val photoAnswerList: List<PhotoAnswerJsonObject>?,

    @SerializedName("all_found")
    val allFound: Boolean?,

    errorCode: ServerErrorCode
) : StatusResponse(errorCode.value) {

    companion object {
        fun success(photoAnswerList: List<PhotoAnswerJsonObject>, allFound: Boolean, errorCode: ServerErrorCode): PhotoAnswerResponse {
            return PhotoAnswerResponse(photoAnswerList, allFound, errorCode)
        }

        fun fail(errorCode: ServerErrorCode): PhotoAnswerResponse {
            return PhotoAnswerResponse(null, null, errorCode)
        }
    }
}