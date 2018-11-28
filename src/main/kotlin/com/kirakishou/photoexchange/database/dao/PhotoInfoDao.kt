package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.model.repo.PhotoInfo
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import reactor.core.publisher.Mono

open class PhotoInfoDao(
  template: ReactiveMongoTemplate
) : BaseDao(template) {
  private val logger = LoggerFactory.getLogger(PhotoInfoDao::class.java)

  override fun create() {
    createCollectionIfNotExists(COLLECTION_NAME)
  }

  override fun clear() {
    dropCollectionIfExists(COLLECTION_NAME)
  }

  fun save(photoInfo: PhotoInfo): Mono<PhotoInfo> {
    return template.save(photoInfo)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(PhotoInfo.empty())
  }

  fun findOldestVacantPhoto(userId: String): Mono<PhotoInfo> {
    val query = Query().with(Sort(Sort.Direction.ASC, PhotoInfo.Mongo.Field.PHOTO_ID))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).ne(userId))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).`is`(PhotoInfo.EMPTY_PHOTO_ID))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.LOCATION_MAP_ID).gt(0L))

    val update = Update()
      .set(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID, PhotoInfo.PHOTO_IS_EXCHANGING)

    return template.findAndModify(query, update, PhotoInfo::class.java)
      .defaultIfEmpty(PhotoInfo.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(PhotoInfo.empty())
  }

  fun resetVacantPhoto(photoId: Long): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

    val update = Update()
      .set(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID, PhotoInfo.EMPTY_PHOTO_ID)

    return template.updateFirst(query, update, PhotoInfo::class.java)
      .map { updateResult -> updateResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun updatePhotoSetReceiverId(photoId: Long, receiverId: Long): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

    val update = Update()
      .set(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID, receiverId)

    return template.updateFirst(query, update, PhotoInfo::class.java)
      .map { updateResult -> updateResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun resetPhotoReceiverId(photoId: Long): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

    val update = Update()
      .set(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID, PhotoInfo.EMPTY_PHOTO_ID)

    return template.updateFirst(query, update, PhotoInfo::class.java)
      .map { updateResult -> updateResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun findPhotosByNames(userId: String, photoNameList: List<String>): Mono<List<PhotoInfo>> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`in`(photoNameList))
      //EXCHANGED_PHOTO_ID should not be -1 or -2
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).gt(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.LOCATION_MAP_ID).ne(PhotoInfo.EMPTY_LOCATION_MAP_ID))
      .limit(photoNameList.size)

    return template.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  fun findManyByIds(photoIdList: List<Long>, sortOrder: SortOrder = SortOrder.Unsorted): Mono<List<PhotoInfo>> {
    val query = when (sortOrder) {
      PhotoInfoDao.SortOrder.Descending -> {
        Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.PHOTO_ID))

      }
      PhotoInfoDao.SortOrder.Ascending -> {
        Query().with(Sort(Sort.Direction.ASC, PhotoInfo.Mongo.Field.PHOTO_ID))
      }
      PhotoInfoDao.SortOrder.Unsorted -> Query()
    }

    query.addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`in`(photoIdList))
      .limit(photoIdList.size)

    return template.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  fun findPage(userId: String, lastUploadedOn: Long, count: Int): Mono<List<PhotoInfo>> {
    val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.UPLOADED_ON))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).lt(lastUploadedOn))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
      .limit(count)

    return template.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  fun findPageOfExchangedPhotos(userId: String, lastUploadedOn: Long, count: Int): Mono<List<PhotoInfo>> {
    val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.UPLOADED_ON))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).gt(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).lt(lastUploadedOn))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
      .limit(count)

    return template.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }


  fun getPhotoIdByName(photoName: String): Mono<Long> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(photoName))

    return template.findOne(query, PhotoInfo::class.java)
      .defaultIfEmpty(PhotoInfo.empty())
      .map { it.photoId }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(-1L)
  }

  fun findById(uploaderPhotoId: Long): Mono<PhotoInfo> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(uploaderPhotoId))

    return template.findOne(query, PhotoInfo::class.java)
      .defaultIfEmpty(PhotoInfo.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(PhotoInfo.empty())
  }

  fun findOneByUserIdAndPhotoName(userId: String, photoName: String): Mono<PhotoInfo> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(photoName))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))

    return template.findOne(query, PhotoInfo::class.java)
      .defaultIfEmpty(PhotoInfo.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(PhotoInfo.empty())
  }

  fun findOlderThan(time: Long): Mono<List<PhotoInfo>> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).lt(time))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).ne(PhotoInfo.EMPTY_PHOTO_ID))

    return template.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }


  fun updateSetLocationMapId(photoId: Long, locationMapId: Long): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

    val update = Update()
      .set(PhotoInfo.Mongo.Field.LOCATION_MAP_ID, locationMapId)

    return template.updateFirst(query, update, PhotoInfo::class.java)
      .map { updateResult -> updateResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun deleteById(photoId: Long): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

    return template.remove(query, PhotoInfo::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun deleteAll(ids: List<Long>): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`in`(ids))
      .limit(ids.size)

    return template.remove(query, PhotoInfo::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  fun photoNameExists(generatedName: String): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(generatedName))

    return template.exists(query, PhotoInfo::class.java)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }


  /**
   * For test purposes
   * */
  fun testFindAll(): Mono<List<PhotoInfo>> {
    return template.findAll(PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  enum class SortOrder {
    Descending,
    Ascending,
    Unsorted
  }

  companion object {
    const val COLLECTION_NAME = "photo_info"
  }
}
























