package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.FileWrapper
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.coobird.thumbnailator.Thumbnails
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.buffer.DataBuffer
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.lang.RuntimeException
import javax.imageio.ImageIO

open class DiskManipulationService {
  private val logger = LoggerFactory.getLogger(DiskManipulationService::class.java)
  private val photoRemovedPlaceholderName = "photo_has_been_removed"
  private val noMapAvailablePlaceholderName = "no_map_available"

  @Throws(IOException::class)
  open fun copyDataBuffersToFile(bufferList: List<DataBuffer>, outFile: File) {
    outFile.outputStream().use { outputStream ->
      for (chunk in bufferList) {
        chunk.asInputStream().use { inputStream ->
          val chunkSize = inputStream.available()
          val buffer = ByteArray(chunkSize)

          //copy chunks from one stream to another
          inputStream.read(buffer, 0, chunkSize)
          outputStream.write(buffer, 0, chunkSize)
        }
      }
    }
  }

  @Throws(IOException::class)
  open suspend fun resizeAndSavePhotos(tempFile: FileWrapper, newUploadingPhoto: PhotoInfo) {
    fun resizeAndSaveImageOnDisk(
      fileWrapper: FileWrapper,
      newMaxSize: Dimension,
      sizeType: String,
      currentFolderDirPath: String,
      imageNewName: String
    ) {
      if (fileWrapper.isEmpty()) {
        throw RuntimeException("No file in the FileWrapper")
      }

      val file = fileWrapper.getFile()!!
      val imageToResize = ImageIO.read(file)
      val outputResizedFile = File("$currentFolderDirPath\\$imageNewName$sizeType")

      checkNotNull(imageToResize)

      //original image size should be bigger than the new size, otherwise we don't need to resize image, just copy it
      if (imageToResize.width > newMaxSize.width || imageToResize.height > newMaxSize.height) {
        val resizedImage = Thumbnails.of(imageToResize)
          .useExifOrientation(false)
          .size(newMaxSize.width, newMaxSize.height)
          .asBufferedImage()

        ImageIO.write(resizedImage, "JPG", outputResizedFile)

      } else {
        file.copyTo(outputResizedFile)
      }
    }

    //save resized (very big) version of the image
    val veryBigDimension = Dimension(ServerSettings.VERY_BIG_PHOTO_SIZE, ServerSettings.VERY_BIG_PHOTO_SIZE)
    resizeAndSaveImageOnDisk(tempFile, veryBigDimension, ServerSettings.VERY_BIG_PHOTO_SUFFIX,
      ServerSettings.FILE_DIR_PATH, newUploadingPhoto.photoName)

    //save resized (big) version of the image
    val bigDimension = Dimension(ServerSettings.BIG_PHOTO_SIZE, ServerSettings.BIG_PHOTO_SIZE)
    resizeAndSaveImageOnDisk(tempFile, bigDimension, ServerSettings.BIG_PHOTO_SUFFIX,
      ServerSettings.FILE_DIR_PATH, newUploadingPhoto.photoName)

    //save resized (medium) version of the image
    val mediumDimension = Dimension(ServerSettings.MEDIUM_PHOTO_SIZE, ServerSettings.MEDIUM_PHOTO_SIZE)
    resizeAndSaveImageOnDisk(tempFile, mediumDimension, ServerSettings.MEDIUM_PHOTO_SUFFIX,
      ServerSettings.FILE_DIR_PATH, newUploadingPhoto.photoName)

    //save resized (small) version of the image
    val smallDimension = Dimension(ServerSettings.SMALL_PHOTO_SIZE, ServerSettings.SMALL_PHOTO_SIZE)
    resizeAndSaveImageOnDisk(tempFile, smallDimension, ServerSettings.SMALL_PHOTO_SUFFIX,
      ServerSettings.FILE_DIR_PATH, newUploadingPhoto.photoName)
  }

  open suspend fun replaceImagesOnDiskWithRemovedImagePlaceholder(photoName: String) {
    fun replacePhotoFile(photoName: String, photoSuffix: String) {
      val path = "files\\photo_removed_image\\$photoRemovedPlaceholderName${photoSuffix}.png"
      val placeholderFile = ClassPathResource(path).file
      if (!placeholderFile.exists()) {
        return
      }

      if (!placeholderFile.isFile) {
        throw RuntimeException("File ${placeholderFile.absolutePath} is not file!")
      }

      if (!placeholderFile.canWrite()) {
        throw RuntimeException("Cannot replace file ${placeholderFile.absolutePath}")
      }

      val targetPath = File("${ServerSettings.FILE_DIR_PATH}\\$photoName${photoSuffix}")
      placeholderFile.copyTo(targetPath, true)
    }

    //replace small photo with placeholder
    replacePhotoFile(photoName, ServerSettings.SMALL_PHOTO_SUFFIX)

    //replace medium photo with placeholder
    replacePhotoFile(photoName, ServerSettings.MEDIUM_PHOTO_SUFFIX)

    //replace big photo with placeholder
    replacePhotoFile(photoName, ServerSettings.BIG_PHOTO_SUFFIX)

    //replace very big photo with placeholder
    replacePhotoFile(photoName, ServerSettings.VERY_BIG_PHOTO_SUFFIX)
  }

  open suspend fun replaceMapOnDiskWithNoMapAvailablePlaceholder(photoName: String) {
    fun replaceMapFile(photoName: String) {
      val path = "files\\photo_removed_image\\$noMapAvailablePlaceholderName.png"
      val placeholderFile = ClassPathResource(path).file
      if (!placeholderFile.exists()) {
        return
      }

      if (!placeholderFile.isFile) {
        throw RuntimeException("File ${placeholderFile.absolutePath} is not file!")
      }

      if (!placeholderFile.canWrite()) {
        throw RuntimeException("Cannot replace file ${placeholderFile.absolutePath}")
      }

      val targetPath = File("${ServerSettings.FILE_DIR_PATH}\\${photoName}_map")
      placeholderFile.copyTo(targetPath, true)
    }

    replaceMapFile(photoName)
  }

  open suspend fun deleteAllPhotoFiles(photoName: String) {
    val photoPath = "${ServerSettings.FILE_DIR_PATH}\\${photoName}"

    val smallPhotoFile = File("$photoPath${ServerSettings.SMALL_PHOTO_SUFFIX}")
    if (smallPhotoFile.exists()) {
      if (!smallPhotoFile.delete()) {
        logger.warn("Could not delete file with path: ${smallPhotoFile.absolutePath}")
      }
    }

    val mediumPhotoFile = File("$photoPath${ServerSettings.MEDIUM_PHOTO_SUFFIX}")
    if (mediumPhotoFile.exists()) {
      if (!mediumPhotoFile.delete()) {
        logger.warn("Could not delete file with path: ${mediumPhotoFile.absolutePath}")
      }
    }

    val bigPhotoFile = File("$photoPath${ServerSettings.BIG_PHOTO_SUFFIX}")
    if (bigPhotoFile.exists()) {
      if (!bigPhotoFile.delete()) {
        logger.warn("Could not delete file with path: ${bigPhotoFile.absolutePath}")
      }
    }

    val veryBigPhotoFile = File("$photoPath${ServerSettings.VERY_BIG_PHOTO_SUFFIX}")
    if (veryBigPhotoFile.exists()) {
      if (!veryBigPhotoFile.delete()) {
        logger.warn("Could not delete file with path: ${veryBigPhotoFile.absolutePath}")
      }
    }

    val mapPhotoFile = File("$photoPath${ServerSettings.PHOTO_MAP_SUFFIX}")
    if (mapPhotoFile.exists()) {
      if (!mapPhotoFile.delete()) {
        logger.warn("Could not delete file with path: ${mapPhotoFile.absolutePath}")
      }
    }
  }

}