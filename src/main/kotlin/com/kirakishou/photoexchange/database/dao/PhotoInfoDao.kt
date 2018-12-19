package com.kirakishou.photoexchange.database.dao

import com.kirakishou.photoexchange.database.entity.PhotoInfo
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoOperations
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

  open fun save(photoInfo: PhotoInfo): Mono<PhotoInfo> {
    return reactiveTemplate.save(photoInfo)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(PhotoInfo.empty())
  }

  open fun findOldestEmptyPhoto(userId: String): Mono<PhotoInfo> {
    val query = Query().with(Sort(Sort.Direction.ASC, PhotoInfo.Mongo.Field.PHOTO_ID))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).ne(userId))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).`is`(PhotoInfo.EMPTY_PHOTO_ID))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.LOCATION_MAP_ID).gt(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).`is`(0L))

    return reactiveTemplate.findOne(query, PhotoInfo::class.java)
      .defaultIfEmpty(PhotoInfo.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(PhotoInfo.empty())
  }

  open fun updatePhotoAsEmpty(photoId: Long): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

    val update = Update()
      .set(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID, PhotoInfo.EMPTY_PHOTO_ID)

    return reactiveTemplate.updateFirst(query, update, PhotoInfo::class.java)
      .map { updateResult -> updateResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun updatePhotoSetReceiverId(photoId: Long, receiverId: Long): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

    val update = Update()
      .set(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID, receiverId)

    return reactiveTemplate.updateFirst(query, update, PhotoInfo::class.java)
      .map { updateResult -> updateResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun findAllByUserId(userId: String): Mono<List<PhotoInfo>> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))

    return reactiveTemplate.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun findPhotosByNames(userId: String, photoNameList: List<String>): Mono<List<PhotoInfo>> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`in`(photoNameList))
      //EXCHANGED_PHOTO_ID should not be -1 or -2
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).gt(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.LOCATION_MAP_ID).ne(PhotoInfo.EMPTY_LOCATION_MAP_ID))
      .limit(photoNameList.size)

    return reactiveTemplate.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun findPhotosByName(photoNameList: List<String>): Mono<List<PhotoInfo>> {
    val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.PHOTO_ID))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`in`(photoNameList))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).`is`(0L))
      .limit(photoNameList.size)

    return reactiveTemplate.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun findManyByIds(photoIdList: List<Long>, sortOrder: SortOrder = SortOrder.Unsorted): Mono<List<PhotoInfo>> {
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
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).`is`(0L))
      .limit(photoIdList.size)

    return reactiveTemplate.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun findPage(userId: String, lastUploadedOn: Long, count: Int): Mono<List<PhotoInfo>> {
    val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.UPLOADED_ON))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).lt(lastUploadedOn))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).`is`(0L))
      .limit(count)

    return reactiveTemplate.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun findPageOfExchangedPhotos(userId: String, lastUploadedOn: Long, count: Int): Mono<List<PhotoInfo>> {
    val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.UPLOADED_ON))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).gt(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).lt(lastUploadedOn))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).`is`(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
      .limit(count)

    return reactiveTemplate.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun findById(uploaderPhotoId: Long): Mono<PhotoInfo> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(uploaderPhotoId))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).`is`(0L))

    return reactiveTemplate.findOne(query, PhotoInfo::class.java)
      .defaultIfEmpty(PhotoInfo.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(PhotoInfo.empty())
  }

  open fun findByPhotoName(photoName: String): Mono<PhotoInfo> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(photoName))

    return reactiveTemplate.findOne(query, PhotoInfo::class.java)
      .defaultIfEmpty(PhotoInfo.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(PhotoInfo.empty())
  }

  open fun findOneByUserIdAndPhotoName(userId: String, photoName: String): Mono<PhotoInfo> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(photoName))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))

    return reactiveTemplate.findOne(query, PhotoInfo::class.java)
      .defaultIfEmpty(PhotoInfo.empty())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(PhotoInfo.empty())
  }

  open fun findPhotosUploadedEarlierThan(earlierThanTime: Long, count: Int): Mono<List<PhotoInfo>> {
    val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.UPLOADED_ON))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).lt(earlierThanTime))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).gt(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).`is`(0L))
      .limit(count)

    return reactiveTemplate.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun findDeletedEarlierThan(earlierThanTime: Long, count: Int): Mono<List<PhotoInfo>> {
    val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.UPLOADED_ON))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).lt(earlierThanTime)
        .andOperator(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).gt(0L)))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).gt(0L))
      .limit(count)

    return reactiveTemplate.find(query, PhotoInfo::class.java)
      .collectList()
      .defaultIfEmpty(emptyList())
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(emptyList())
  }

  open fun updateSetLocationMapId(photoId: Long, locationMapId: Long): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

    val update = Update()
      .set(PhotoInfo.Mongo.Field.LOCATION_MAP_ID, locationMapId)

    return reactiveTemplate.updateFirst(query, update, PhotoInfo::class.java)
      .map { updateResult -> updateResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun updateManySetDeletedOn(now: Long, toBeUpdated: List<Long>): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`in`(toBeUpdated))

    val update = Update()
      .set(PhotoInfo.Mongo.Field.DELETED_ON, now)

    return reactiveTemplate.updateMulti(query, update, PhotoInfo::class.java)
      .map { updateResult -> updateResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun deleteById(photoId: Long, template: ReactiveMongoOperations = reactiveTemplate): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`is`(photoId))

    return template.remove(query, PhotoInfo::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun deleteAll(ids: List<Long>): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).`in`(ids))
      .limit(ids.size)

    return reactiveTemplate.remove(query, PhotoInfo::class.java)
      .map { deletionResult -> deletionResult.wasAcknowledged() }
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun photoNameExists(generatedName: String): Mono<Boolean> {
    val query = Query()
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_NAME).`is`(generatedName))

    return reactiveTemplate.exists(query, PhotoInfo::class.java)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(false)
  }

  open fun countFreshUploadedPhotosSince(userId: String, time: Long): Mono<Int> {
    val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.PHOTO_ID))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.PHOTO_ID).gt(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).gt(time))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).`is`(0L))

    return reactiveTemplate.count(query, PhotoInfo::class.java)
      .defaultIfEmpty(0L)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(0L)
      .map { it.toInt() }
  }

  open fun countFreshExchangedPhotos(userId: String, time: Long): Mono<Int> {
    val query = Query().with(Sort(Sort.Direction.DESC, PhotoInfo.Mongo.Field.UPLOADED_ON))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.EXCHANGED_PHOTO_ID).gt(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.UPLOADED_ON).gt(time))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.DELETED_ON).`is`(0L))
      .addCriteria(Criteria.where(PhotoInfo.Mongo.Field.USER_ID).`is`(userId))

    return reactiveTemplate.count(query, PhotoInfo::class.java)
      .defaultIfEmpty(0L)
      .doOnError { error -> logger.error("DB error", error) }
      .onErrorReturn(0L)
      .map { it.toInt() }
  }

  /**
   * For test purposes
   * */

  open fun testFindAll(): Mono<List<PhotoInfo>> {
    return reactiveTemplate.findAll(PhotoInfo::class.java)
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
























