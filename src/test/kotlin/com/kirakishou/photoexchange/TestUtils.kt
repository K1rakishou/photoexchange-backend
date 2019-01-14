package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.util.SecurityUtils
import com.kirakishou.photoexchange.util.TimeUtils
import org.joda.time.DateTime
import java.util.*

object TestUtils {

  fun createPhoto(
    photoId: Long,
    exchangeState: ExchangeState,
    userId: Long,
    exchangedPhotoId: Long,
    locationMapId: Long,
    photoName: String,
    isPublic: Boolean,
    lon: Double,
    lat: Double,
    uploadedOn: Long,
    deletedOn: Long,
    ipHash: String
  ): Photo {
    return Photo(
      PhotoId(photoId),
      exchangeState,
      UserId(userId),
      ExchangedPhotoId(exchangedPhotoId),
      LocationMapId(locationMapId),
      PhotoName(photoName),
      isPublic,
      lon,
      lat,
      DateTime(uploadedOn),
      DateTime(deletedOn),
      IpHash(ipHash)
    )
  }

  fun createGalleryPhoto(
    galleryPhotoId: Long,
    photoId: Long,
    uploadedOn: Long
  ): GalleryPhoto {
    return GalleryPhoto(
      GalleryPhotoId(galleryPhotoId),
      PhotoId(photoId),
      uploadedOn
    )
  }

  fun createExchangedPhotoPairs(
    count: Int,
    userIdList: List<Long>,
    privatePhotosAmount: Int = 0
  ): List<Photo> {
    require(count >= privatePhotosAmount)

    val random = Random()
    val resultList = mutableListOf<Photo>()
    val photosVisibilityList = generatePhotoVisibilityList(count, privatePhotosAmount)

    for (i in 0 until count step 2) {
      val currentPhotoId = i.toLong() + 1
      val exchangedPhotoId = (i + 2).toLong()
      val locationMapId = i.toLong()
      val uploadedOn = i.toLong() + 100

      resultList += Photo(
        PhotoId(currentPhotoId),
        ExchangeState.Exchanged,
        UserId(userIdList[i % 2]),
        ExchangedPhotoId(exchangedPhotoId),
        LocationMapId(locationMapId + 1),
        PhotoName("photoName_${SecurityUtils.Generation.generateRandomString(10)}"),
        photosVisibilityList[i],
        random.nextDouble() * 45.0,
        random.nextDouble() * 45.0,
        DateTime(uploadedOn),
        TimeUtils.dateTimeZero,
        IpHash("ipHash_${SecurityUtils.Generation.generateRandomString(10)}")
      )

      resultList += Photo(
        PhotoId(exchangedPhotoId),
        ExchangeState.Exchanged,
        UserId(userIdList[(i + 1) % 2]),
        ExchangedPhotoId(currentPhotoId),
        LocationMapId(locationMapId + 2),
        PhotoName("photoName_${SecurityUtils.Generation.generateRandomString(10)}"),
        photosVisibilityList[i + 1],
        random.nextDouble() * 45.0,
        random.nextDouble() * 45.0,
        DateTime(uploadedOn + 1),
        TimeUtils.dateTimeZero,
        IpHash("ipHash_${SecurityUtils.Generation.generateRandomString(10)}")
      )
    }

    return resultList
  }

  private fun generatePhotoVisibilityList(count: Int, privatePhotosAmount: Int): List<Boolean> {
    val sequence = (0 until count)
      .asSequence()

    val result = mutableListOf<Boolean>()

    result += sequence.take(privatePhotosAmount)
      .map { false }
      .toList()

    result += sequence.drop(privatePhotosAmount)
      .map { true }
      .toList()

    return result.shuffled()
  }

}