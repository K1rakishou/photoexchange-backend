package com.kirakishou.photoexchange.model

enum class ServerErrorCode(val value: Int) {
    UNKNOWN_ERROR(-1),
    OK(0),
    BAD_REQUEST(1),
    REPOSITORY_ERROR(2),
    DISK_ERROR(3),
    USER_HAS_NO_UPLOADED_PHOTOS(4),
    NO_PHOTOS_TO_SEND_BACK(5)
}