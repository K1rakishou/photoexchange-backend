package com.kirakishou.photoexchange.model

class ErrorCode {

	enum class TakePhotoErrors(val value: Int) {
		UnknownError(0),
		Ok(1),
		CameraIsNotAvailable(2),
		CameraIsNotStartedException(3),
		TimeoutException(4),
		DatabaseError(5),
		CouldNotTakePhoto(6);
	}

	enum class UploadPhotoErrors(val value: Int) {
		UnknownError(100),
		Ok(101),
		BadRequest(102),
		DatabaseError(103),

		LocalBadServerResponse(125),
		LocalNoPhotoFileOnDisk(126),
		LocalTimeout(127),
		LocalInterrupted(128),
		LocalDatabaseError(129),
		LocalCouldNotGetUserId(130);
	}

	enum class ReceivePhotosErrors(val value: Int) {
		UnknownError(200),
		Ok(201),
		BadRequest(202),
		NoPhotosInRequest(203),
		NoPhotosToSendBack(204),

		LocalDatabaseError(225),
		LocalTooManyPhotosRequested(226),
		LocalNotEnoughPhotosUploaded(227),
		LocalBadServerResponse(228),
		LocalTimeout(229);
	}

	enum class GalleryPhotosErrors(val value: Int) {
		UnknownError(300),
		Ok(301),
		BadRequest(302),
		NoPhotosInRequest(303),

		LocalBadServerResponse(325),
		LocalTimeout(326),
		LocalDatabaseError(327);
	}

	enum class FavouritePhotoErrors(val value: Int) {
		UnknownError(400),
		Ok(401),
		BadRequest(402),

		LocalBadServerResponse(425),
		LocalTimeout(426);
	}

	enum class ReportPhotoErrors(val value: Int) {
		UnknownError(0),
		Ok(1),
		BadRequest(2),

		LocalBadServerResponse(25),
		LocalTimeout(26),
	}

	enum class GetUserIdError(val value: Int) {
		UnknownError(500),
		Ok(501),
		DatabaseError(502),

		LocalBadServerResponse(525),
		LocalTimeout(526),
		LocalDatabaseError(527);
	}

	enum class GetUploadedPhotosError(val value: Int) {
		UnknownError(600),
		Ok(601),
		DatabaseError(602),
		BadRequest(603),
		NoPhotosInRequest(604),

		LocalBadServerResponse(625),
		LocalTimeout(626),
		LocalUserIdIsEmpty(627);
	}
}