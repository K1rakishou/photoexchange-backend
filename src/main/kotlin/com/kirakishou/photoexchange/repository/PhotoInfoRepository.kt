package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.PhotoInfo
import com.kirakishou.photoexchange.util.TimeUtils
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import reactor.core.publisher.Mono

open class PhotoInfoRepository(private val template: ReactiveMongoTemplate,
                               private val mongoSequenceRepo: MongoSequenceRepository) {

    fun save(photoInfoParam: PhotoInfo): Mono<PhotoInfo> {
        return mongoSequenceRepo.getNextId(SEQUENCE_NAME)
                .zipWith(Mono.just(photoInfoParam))
                .map {
                    val id = it.t1
                    val photoInfo = it.t2
                    photoInfo.photoId = id

                    return@map photoInfo
                }
                .flatMap { photoInfo ->
                    template.save(photoInfo)
                            .onErrorReturn(PhotoInfo.empty())
                }
    }

    fun countUserUploadedPhotos(userId: String): Mono<Long> {
        val query = Query()
                .addCriteria(Criteria.where("whoUploaded").`is`(userId))
                .addCriteria(Criteria.where("receivedPhotoBack").`is`(false))
                .addCriteria(Criteria.where("candidateFound").`is`(false))

        return template.count(query, PhotoInfo::class.java)
    }

    fun findPhotoInfo(userId: String): Mono<PhotoInfo> {
        val query = Query().with(Sort(Sort.Direction.ASC, "uploadedOn"))
                .addCriteria(Criteria.where("whoUploaded").ne(userId))
                .addCriteria(Criteria.where("receivedPhotoBack").`is`(false))
                .addCriteria(Criteria.where("candidateFound").`is`(false))
                .limit(1)

        val update = Update()
                .set("candidateFound", true)
                .set("candidateFoundOn", TimeUtils.getTime())

        return template.findAndModify(query, update, PhotoInfo::class.java)
                .switchIfEmpty(Mono.just(PhotoInfo.empty()))
    }

    fun updateSetPhotoSuccessfullyDelivered(userId: String) {
        TODO()
    }

    fun deleteById(userId: String) {
        template.remove(Query.query(Criteria.where("whoUploaded").`is`(userId)))
    }

    companion object {
        const val COLLECTION_NAME = "photo_info"
        const val SEQUENCE_NAME = "photo_info_sequence"
    }
}
























