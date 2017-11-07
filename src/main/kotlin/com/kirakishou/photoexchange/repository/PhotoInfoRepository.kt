package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.model.PhotoInfo
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux
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

    fun findPhotoInfo(userId: String): Mono<PhotoInfo> {
        val criteria = Criteria
                .where("whoUploaded").`is`(userId).not()
                .andOperator(Criteria.where("receivedPhotoBack").`is`(false))

        val query = Query().with(Sort(Sort.Direction.DESC, "uploadedOn"))
                .addCriteria(criteria)

        return template.find(query, PhotoInfo::class.java)
                .switchIfEmpty(Flux.just(PhotoInfo.empty()))
                .single()
    }

    fun deleteById(userId: String) {
        template.remove(Query.query(Criteria.where("whoUploaded").`is`(userId)))
    }

    companion object {
        const val COLLECTION_NAME = "photo_info"
        const val SEQUENCE_NAME = "photo_info_sequence"
    }
}