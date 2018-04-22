package com.kirakishou.photoexchange.model.net.response

import com.google.gson.annotations.SerializedName
import com.kirakishou.photoexchange.model.ErrorCode

class PhotoAnswerResponse
private constructor(

	@SerializedName("photo_answers")
	val photoAnswers: List<PhotoAnswer>,

	errorCode: ErrorCode.GetPhotoAnswerErrors
) : StatusResponse(errorCode.value) {

	companion object {
		fun success(photoAnswerList: List<PhotoAnswer>): PhotoAnswerResponse {
			return PhotoAnswerResponse(photoAnswerList, ErrorCode.GetPhotoAnswerErrors.Ok())
		}

		fun fail(errorCode: ErrorCode.GetPhotoAnswerErrors): PhotoAnswerResponse {
			return PhotoAnswerResponse(emptyList(), errorCode)
		}
	}
}