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

	sealed class GetGalleryPhotosErrors(value: Int) : ErrorCode(value) {
		class UnknownError : GetGalleryPhotosErrors(-1)
		class Ok : GetGalleryPhotosErrors(0)
		class BadRequest : GetGalleryPhotosErrors(1)
	}
}