package com.kirakishou.photoexchange.database.entity

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

	@Field(Mongo.Field.ATTEMPTS_COUNT)
	var attemptsCount: Int,

	@Field(Mongo.Field.MAP_STATUS)
	var mapStatus: Int,

	@Field(Mongo.Field.NEXT_ATTEMPT_TIME)
	var nextAttemptTime: Long
) {

	fun isEmpty(): Boolean {
		return id == -1L
	}

	companion object {
		fun empty(): LocationMap {
			return LocationMap(-1L, -1L, 0, MapStatus.Empty.value, 0L)
		}

		fun create(photoId: Long): LocationMap {
			return LocationMap(-1L, photoId, 0, MapStatus.Empty.value, 0L)
		}
	}

	enum class MapStatus(val value: Int) {
		Empty(0),
		Ready(1),
		Failed(2)
	}

	object Mongo {
		object Field {
			const val ID = "_id"
			const val PHOTO_ID = "photo_id"
			const val ATTEMPTS_COUNT = "attemps_count"
			const val MAP_STATUS = "map_status"
			const val NEXT_ATTEMPT_TIME = "next_attempt_time"
		}

		object Index {
			const val PHOTO_ID = "photo_id_index"
		}
	}
}