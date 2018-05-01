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

	sealed class GalleryPhotosErrors(value: Int) : ErrorCode(value) {
		class UnknownError : GalleryPhotosErrors(-1)
		class Ok : GalleryPhotosErrors(0)
		class BadRequest : GalleryPhotosErrors(1)
	}

	sealed class FavouritePhotoErrors(value: Int) : ErrorCode(value) {
		class UnknownError : FavouritePhotoErrors(-1)
		class Ok : FavouritePhotoErrors(0)
		class AlreadyFavourited : FavouritePhotoErrors(1)
	}
}