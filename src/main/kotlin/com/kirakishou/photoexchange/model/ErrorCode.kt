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
		UnknownError(50),
		Ok(51),
		BadRequest(52),
		DatabaseError(53);
	}

	enum class ReceivePhotosErrors(val value: Int) {
		UnknownError(100),
		Ok(101),
		BadRequest(102),
		NoPhotosInRequest(103),
		NoPhotosToSendBack(104);
	}

	enum class GalleryPhotosErrors(val value: Int) {
		UnknownError(150),
		Ok(151),
		BadRequest(152),
		NoPhotosInRequest(153);
	}

	enum class FavouritePhotoErrors(val value: Int) {
		UnknownError(200),
		Ok(201),
		BadRequest(202);
	}

	enum class ReportPhotoErrors(val value: Int) {
		UnknownError(250),
		Ok(251),
		BadRequest(252);
	}

	enum class GetUserIdErrors(val value: Int) {
		UnknownError(300),
		Ok(301),
		DatabaseError(302);
	}

	enum class GetUploadedPhotosErrors(val value: Int) {
		UnknownError(350),
		Ok(351),
		DatabaseError(352),
		BadRequest(353),
		NoPhotosInRequest(354);
	}

	enum class GetReceivedPhotosErrors(val value: Int) {
		UnknownError(400),
		Ok(401),
		DatabaseError(402),
		BadRequest(403),
		NoPhotosInRequest(404);
	}
}