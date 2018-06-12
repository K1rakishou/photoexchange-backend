package com.kirakishou.photoexchange.model.repo

import com.kirakishou.photoexchange.database.dao.LocationMapDao
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = LocationMapDao.COLLECTION_NAME)
class LocationMap(
	@Id
	@Field(Mongo.Field.ID)
	var id: Long,

	@Indexed(name = Mongo.Index.PHOTO_ID)
	@Field(Mongo.Field.PHOTO_ID)
	var photoId: Long,

	@Field(Mongo.Field.MAP_READY)
	var mapReady: Boolean
) {

	fun isEmpty(): Boolean {
		return id == -1L
	}

	companion object {
		fun empty(): LocationMap {
			return LocationMap(-1L, -1L, false)
		}

		fun create(photoId: Long): LocationMap {
			return LocationMap(-1L, photoId, false)
		}
	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val PHOTO_ID = "photo_id"
			const val MAP_READY = "map_ready"
		}

		object Index {
			const val PHOTO_ID = "photo_id_index"
		}
	}
}