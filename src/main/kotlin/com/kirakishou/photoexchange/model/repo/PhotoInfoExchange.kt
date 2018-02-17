package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.repository.PhotoInfoExchangeRepository
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = PhotoInfoExchangeRepository.COLLECTION_NAME)
data class PhotoInfoExchange(
    @Id
    var exchangeId: Long,

    @Indexed(name = "uploader_photo_info_id_index")
    var uploaderPhotoInfoId: Long,

    @Indexed(name = "receiver_photo_info_id_index")
    var receiverPhotoInfoId: Long,

    var uploaderOk: Boolean,
    var receiverOk: Boolean
) {

    fun isEmpty(): Boolean {
        return exchangeId == -1L
    }

    companion object {
        fun empty(): PhotoInfoExchange {
            return PhotoInfoExchange(-1L, 0L, 0L, false, false)
        }
    }
}