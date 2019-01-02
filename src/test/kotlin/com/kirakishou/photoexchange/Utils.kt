package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.core.*
import com.kirakishou.photoexchange.util.SecurityUtils
import java.util.*

object Utils {

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
        UserId(userIdList[i % 2]),
        ExchangedPhotoId(exchangedPhotoId),
        LocationMapId(locationMapId + 1),
        PhotoName("photoName_${SecurityUtils.Generation.generateRandomString(10)}"),
        photosVisibilityList[i],
        random.nextDouble() * 45.0,
        random.nextDouble() * 45.0,
        uploadedOn,
        0L,
        IpHash("ipHash_${SecurityUtils.Generation.generateRandomString(10)}")
      )

      resultList += Photo(
        PhotoId(exchangedPhotoId),
        UserId(userIdList[(i + 1) % 2]),
        ExchangedPhotoId(currentPhotoId),
        LocationMapId(locationMapId + 2),
        PhotoName("photoName_${SecurityUtils.Generation.generateRandomString(10)}"),
        photosVisibilityList[i + 1],
        random.nextDouble() * 45.0,
        random.nextDouble() * 45.0,
        uploadedOn + 1,
        0L,
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