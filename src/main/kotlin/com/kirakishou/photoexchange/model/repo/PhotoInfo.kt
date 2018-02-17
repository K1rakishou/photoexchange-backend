package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.IndexDirection
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = PhotoInfoDao.COLLECTION_NAME)
class PhotoInfo private constructor(
    @Id
    @Field(Mongo.Field.PHOTO_ID)
    var photoId: Long,

    @Indexed(name = Mongo.Index.WHO_UPLOADED)
    @Field(Mongo.Field.WHO_UPLOADED)
    var whoUploaded: String,

    @Indexed(name = Mongo.Index.PHOTO_NAME)
    @Field(Mongo.Field.PHOTO_NAME)
    val photoName: String,

    @Field(Mongo.Field.LONGITUDE)
    val lon: Double,

    @Field(Mongo.Field.LATITUDE)
    val lat: Double,

    @Indexed(name = Mongo.Index.UPLOADED_ON, direction = IndexDirection.DESCENDING)
    @Field(Mongo.Field.UPLOADED_ON)
    val uploadedOn: Long
) {
    fun isEmpty(): Boolean {
        return photoId == -1L
    }

    companion object {
        fun empty(): PhotoInfo {
            return PhotoInfo(-1L, "", "", 0.0, 0.0, 0L)
        }

        fun create(userId: String, photoName: String, lon: Double, lat: Double, time: Long): PhotoInfo {
            return PhotoInfo(-1, userId, photoName, lon, lat, time)
        }
    }

    object Mongo {
        object Field {
            const val PHOTO_ID = "photo_id"
            const val WHO_UPLOADED = "who_uploaded"
            const val PHOTO_NAME = "photo_name"
            const val LONGITUDE = "longitude"
            const val LATITUDE = "latitude"
            const val UPLOADED_ON = "uploaded_on"
        }

        object Index {
            const val WHO_UPLOADED = "who_uploaded_index"
            const val PHOTO_NAME = "photo_name_index"
            const val UPLOADED_ON = "uploaded_on_index"
        }
    }
}