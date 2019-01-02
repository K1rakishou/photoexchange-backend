package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.core.FileWrapper
import com.kirakishou.photoexchange.core.Photo
import com.kirakishou.photoexchange.core.PhotoName
import net.coobird.thumbnailator.Thumbnails
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.buffer.DataBuffer
import java.awt.Dimension
import java.io.File
import java.io.IOException
import java.nio.file.Files
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
  open suspend fun resizeAndSavePhotos(tempFile: FileWrapper, newUploadingPhoto: Photo) {
    fun resizeAndSaveImageOnDisk(
      fileWrapper: FileWrapper,
      newMaxSize: Dimension,
      sizeType: String,
      currentFolderDirPath: String,
      imageNewName: PhotoName
    ) {
      if (fileWrapper.isEmpty()) {
        throw RuntimeException("No file in the FileWrapper")
      }

      val file = fileWrapper.getFile()!!
      val imageToResize = ImageIO.read(file)
      val outputResizedFile = File("$currentFolderDirPath\\${imageNewName.name}$sizeType")

      checkNotNull(imageToResize)

      //original image size should be bigger than the new size
      if (imageToResize.width > newMaxSize.width || imageToResize.height > newMaxSize.height) {
        val resizedImage = Thumbnails.of(imageToResize)
          .useExifOrientation(false)
          .size(newMaxSize.width, newMaxSize.height)
          .asBufferedImage()

        ImageIO.write(resizedImage, "JPG", outputResizedFile)
      } else {
        //otherwise we don't need to resize image, just copy it
        copyFileTo(file, outputResizedFile, true)
      }
    }

    //save resized (very big) version of the image
    resizeAndSaveImageOnDisk(
      tempFile,
      Dimension(ServerSettings.VERY_BIG_PHOTO_SIZE, ServerSettings.VERY_BIG_PHOTO_SIZE),
      ServerSettings.VERY_BIG_PHOTO_SUFFIX,
      ServerSettings.FILE_DIR_PATH,
      newUploadingPhoto.photoName
    )

    //save resized (big) version of the image
    resizeAndSaveImageOnDisk(
      tempFile,
      Dimension(ServerSettings.BIG_PHOTO_SIZE, ServerSettings.BIG_PHOTO_SIZE),
      ServerSettings.BIG_PHOTO_SUFFIX,
      ServerSettings.FILE_DIR_PATH,
      newUploadingPhoto.photoName
    )

    //save resized (medium) version of the image
    resizeAndSaveImageOnDisk(
      tempFile,
      Dimension(ServerSettings.MEDIUM_PHOTO_SIZE, ServerSettings.MEDIUM_PHOTO_SIZE),
      ServerSettings.MEDIUM_PHOTO_SUFFIX,
      ServerSettings.FILE_DIR_PATH,
      newUploadingPhoto.photoName
    )

    //save resized (small) version of the image
    resizeAndSaveImageOnDisk(
      tempFile,
      Dimension(ServerSettings.SMALL_PHOTO_SIZE, ServerSettings.SMALL_PHOTO_SIZE),
      ServerSettings.SMALL_PHOTO_SUFFIX,
      ServerSettings.FILE_DIR_PATH,
      newUploadingPhoto.photoName
    )
  }

  @Throws(IOException::class)
  open suspend fun replaceImagesOnDiskWithRemovedImagePlaceholder(photoName: PhotoName) {
    fun replacePhotoFile(photoName: PhotoName, photoSuffix: String) {
      val path = "files\\photo_removed_image\\$photoRemovedPlaceholderName${photoSuffix}.png"

      val resource = ClassPathResource(path)
      if (!resource.exists()) {
        logger.debug("Resource with path ($path) does not exist")
        return
      }

      val placeholderFile = resource.file
      if (!placeholderFile.exists()) {
        logger.debug("Placeholder file (${placeholderFile.absolutePath}) does not exist")
        return
      }

      val targetFile = File("${ServerSettings.FILE_DIR_PATH}\\${photoName.name}${photoSuffix}")
      copyFileTo(placeholderFile, targetFile, true)
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

  @Throws(IOException::class)
  open suspend fun replaceMapOnDiskWithNoMapAvailablePlaceholder(photoName: PhotoName) {
    val path = "files\\photo_removed_image\\$noMapAvailablePlaceholderName.png"

    val resource = ClassPathResource(path)
    if (!resource.exists()) {
      logger.debug("Resource with path ($path) does not exist")
      return
    }

    val placeholderFile = resource.file
    if (!placeholderFile.exists()) {
      logger.debug("Placeholder file (${placeholderFile.absolutePath}) does not exist")
      return
    }

    val targetFile = File("${ServerSettings.FILE_DIR_PATH}\\${photoName.name}_map")
    copyFileTo(placeholderFile, targetFile, true)
  }

  @Throws(IOException::class)
  open suspend fun deleteAllPhotoFiles(photoName: PhotoName) {
    val photoPath = "${ServerSettings.FILE_DIR_PATH}\\${photoName.name}"

    deleteFile(File("$photoPath${ServerSettings.SMALL_PHOTO_SUFFIX}"))
    deleteFile(File("$photoPath${ServerSettings.MEDIUM_PHOTO_SUFFIX}"))
    deleteFile(File("$photoPath${ServerSettings.BIG_PHOTO_SUFFIX}"))
    deleteFile(File("$photoPath${ServerSettings.VERY_BIG_PHOTO_SUFFIX}"))
    deleteFile(File("$photoPath${ServerSettings.PHOTO_MAP_SUFFIX}"))
  }

  @Throws(IOException::class)
  private fun copyFileTo(srcFile: File, targetFile: File, overwrite: Boolean) {
    if (!srcFile.isFile) {
      throw IOException("srcFile is not a file! path = ${srcFile.absolutePath}")
    }

    if (!srcFile.canRead()) {
      throw IOException("Cannot read srcFile! path = ${srcFile.absolutePath}")
    }

    if (targetFile.exists()) {
      if (!targetFile.isFile) {
        throw IOException("targetFile is not a file! path = ${targetFile.absolutePath}")
      }

      if (!targetFile.canWrite()) {
        throw IOException("Cannot write to targetFile! path = ${targetFile.absolutePath}")
      }
    }

    srcFile.copyTo(targetFile, overwrite)
  }

  @Throws(IOException::class)
  private fun deleteFile(fileToDelete: File) {
    if (!fileToDelete.exists()) {
      logger.debug("File (${fileToDelete.absolutePath}) does not exist")
      return
    }

    if (!fileToDelete.isFile) {
      throw IOException("fileToDelete is not a file! path = ${fileToDelete.absolutePath}")
    }

    Files.deleteIfExists(fileToDelete.toPath())
  }

}