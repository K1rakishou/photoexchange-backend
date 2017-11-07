package com.kirakishou.photoexchange.model

import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = PhotoInfoRepository.COLLECTION_NAME)
data class PhotoInfo(@Id
                     var photoId: Long,

                     @Indexed(name = "who_uploaded")
                     val whoUploaded: String,

                     @Indexed(name = "photo_name_index")
                     val photoName: String,

                     val lon: Double,
                     val lat: Double,
                     val receivedPhotoBack: Boolean,

                     @Indexed(name = "uploaded_on", direction = IndexDirection.DESCENDING)
                     val uploadedOn: Long) {

    fun isEmpty(): Boolean {
        return photoId == -1L
                && whoUploaded.isEmpty()
                && photoName.isEmpty()
                && lon == 0.0
                && lat == 0.0
                && !receivedPhotoBack
                && uploadedOn == 0L
    }

    companion object {
        fun empty(): PhotoInfo {
            return PhotoInfo(-1L, "", "", 0.0, 0.0, false, 0L)
        }
    }
}