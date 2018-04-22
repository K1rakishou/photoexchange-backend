package com.kirakishou.photoexchange.model

sealed class ErrorCode(val value: Int) {

	fun toInt(): Int {
		return this.value
	}

	sealed class UploadPhotoErrors(value: Int) : ErrorCode(value) {
		class UnknownError : UploadPhotoErrors(-1)
		class Ok : UploadPhotoErrors(0)
		class BadRequest : UploadPhotoErrors(1)
		class DatabaseError : UploadPhotoErrors(2)
	}

	sealed class GetPhotoAnswerErrors(value: Int) : ErrorCode(value) {
		class UnknownError : GetPhotoAnswerErrors(-1)
		class Ok : GetPhotoAnswerErrors(0)
		class BadRequest : GetPhotoAnswerErrors(1)
		class DatabaseError : GetPhotoAnswerErrors(2)
		class NoPhotosInRequest : GetPhotoAnswerErrors(3)
		class TooManyPhotosRequested : GetPhotoAnswerErrors(4)
		class NoPhotosToSendBack : GetPhotoAnswerErrors(5)
		class NotEnoughPhotosUploaded : GetPhotoAnswerErrors(6)
	}

	sealed class MarkPhotoAsReceivedErrors(value: Int) : ErrorCode(value) {
		class UnknownError : MarkPhotoAsReceivedErrors(-1)
		class Ok : MarkPhotoAsReceivedErrors(0)
		class BadRequest : MarkPhotoAsReceivedErrors(1)
		class BadPhotoId : MarkPhotoAsReceivedErrors(2)
		class PhotoInfoNotFound : MarkPhotoAsReceivedErrors(3)
		class PhotoInfoExchangeNotFound : MarkPhotoAsReceivedErrors(4)
		class UpdateError : MarkPhotoAsReceivedErrors(5)
	}
}