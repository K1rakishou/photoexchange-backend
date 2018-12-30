package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.util.SecurityUtils
import java.util.*

object Utils {

  fun createExchangedPhotoPairs(
    count: Int,
    userIdList: List<String>,
    privatePhotosAmount: Int = 0
  ): List<PhotoInfo> {
    require(count >= privatePhotosAmount)
    require(userIdList.size % 2 == 0)

    val random = Random()
    val resultList = mutableListOf<PhotoInfo>()
    val photosVisibilityList = generatePhotoVisibilityList(count, privatePhotosAmount)

    for (i in 0 until count step 2) {
      val currentPhotoId = i.toLong() + 1
      val exchangedPhotoId = (i + 2).toLong()
      val locationMapId = i.toLong()
      val uploadedOn = i.toLong() + 100

      resultList += PhotoInfo(
        currentPhotoId,
        exchangedPhotoId,
        locationMapId + 1,
        userIdList[i % 2],
        "photoName_${SecurityUtils.Generation.generateRandomString(10)}",
        photosVisibilityList[i],
        random.nextDouble() * 45.0,
        random.nextDouble() * 45.0,
        uploadedOn,
        0L,
        "ipHash_${SecurityUtils.Generation.generateRandomString(10)}"
      )

      resultList += PhotoInfo(
        exchangedPhotoId,
        currentPhotoId,
        locationMapId + 2,
        userIdList[(i + 1) % 2],
        "photoName_${SecurityUtils.Generation.generateRandomString(10)}",
        photosVisibilityList[i + 1],
        random.nextDouble() * 45.0,
        random.nextDouble() * 45.0,
        uploadedOn + 1,
        0L,
        "ipHash_${SecurityUtils.Generation.generateRandomString(10)}"
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