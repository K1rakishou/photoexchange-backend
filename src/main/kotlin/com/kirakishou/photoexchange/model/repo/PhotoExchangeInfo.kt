package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.repository.PhotoExchangeInfoRepository
import com.kirakishou.photoexchange.util.TimeUtils
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = PhotoExchangeInfoRepository.COLLECTION_NAME)
data class PhotoExchangeInfo(
    @Id
    var photoExchangeInfoId: Long,

    @Indexed(name = "uploader_photo_id_index")
    val uploaderPhotoId: Long = -1L,

    @Indexed(name = "receiver_photo_id_index")
    val receiverPhotoId: Long = -1L,

    @Indexed(name = "created_on_index", direction = IndexDirection.DESCENDING)
    val createdOn: Long
) {
    fun isEmpty(): Boolean {
        return photoExchangeInfoId == -1L
    }

    companion object {
        fun empty(): PhotoExchangeInfo {
            return PhotoExchangeInfo(-1L, -1L, -1L, 0L)
        }

        fun create(uploaderPhotoId: Long): PhotoExchangeInfo {
            return PhotoExchangeInfo(
                    -1L,
                    uploaderPhotoId,
                    -1L,
                    TimeUtils.getTimeFast())
        }
    }
}