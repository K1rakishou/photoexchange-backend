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

	sealed class GalleryPhotoIdsErrors(value: Int) : ErrorCode(value) {
		class UnknownError : GalleryPhotoIdsErrors(-1)
		class Ok : GalleryPhotoIdsErrors(0)
		class BadRequest : GalleryPhotoIdsErrors(1)
	}

	sealed class GalleryPhotosErrors(value: Int) : ErrorCode(value) {
		class UnknownError : GalleryPhotosErrors(-1)
		class Ok : GalleryPhotosErrors(0)
		class BadRequest : GalleryPhotosErrors(1)
		class NoPhotosInRequest : GalleryPhotosErrors(2)
	}

	sealed class GalleryPhotosInfoError(value: Int) : ErrorCode(value) {
		class UnknownError : GalleryPhotosInfoError(-1)
		class Ok : GalleryPhotosInfoError(0)
		class BadRequest : GalleryPhotosInfoError(1)
		class NoPhotosInRequest : GalleryPhotosInfoError(2)
	}

	sealed class FavouritePhotoErrors(value: Int) : ErrorCode(value) {
		class UnknownError : FavouritePhotoErrors(-1)
		class Ok : FavouritePhotoErrors(0)
		class AlreadyFavourited : FavouritePhotoErrors(1)
		class BadRequest : FavouritePhotoErrors(2)
	}

	sealed class ReportPhotoErrors(value: Int) : ErrorCode(value) {
		class UnknownError : ReportPhotoErrors(-1)
		class Ok : ReportPhotoErrors(0)
		class AlreadyReported : ReportPhotoErrors(1)
		class BadRequest : ReportPhotoErrors(2)
	}

	sealed class GetUserIdError(value: Int) : ErrorCode(value) {
		class UnknownError : GetUserIdError(-1)
		class Ok : GetUserIdError(0)
		class DatabaseError : GetUserIdError(1)
	}

	sealed class GetUploadedPhotoIdsError(value: Int) : ErrorCode(value) {
		class UnknownError : GetUploadedPhotoIdsError(-1)
		class Ok : GetUploadedPhotoIdsError(0)
		class DatabaseError : GetUploadedPhotoIdsError(1)
		class BadRequest : GetUploadedPhotoIdsError(2)
	}

	sealed class GetUploadedPhotosError(value: Int) : ErrorCode(value) {
		class UnknownError : GetUploadedPhotosError(-1)
		class Ok : GetUploadedPhotosError(0)
		class DatabaseError : GetUploadedPhotosError(1)
		class BadRequest : GetUploadedPhotosError(2)
		class NoPhotosInRequest : GetUploadedPhotosError(3)
	}
}