package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.entity.PhotoInfo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.coobird.thumbnailator.Thumbnails
import org.springframework.core.io.ClassPathResource
import java.awt.Dimension
import java.io.File
import java.lang.RuntimeException
import javax.imageio.ImageIO

open class ImageManipulationService {
	private val mutex = Mutex()
	private val photoRemovedPlaceHolderName = "photo_has_been_removed"

	open suspend fun resizeAndSavePhotos(tempFile: File, newUploadingPhoto: PhotoInfo) {
		fun resizeAndSaveImageOnDisk(
			file: File,
			newMaxSize: Dimension,
			sizeType: String,
			currentFolderDirPath: String,
			imageNewName: String
		) {
			check(file.exists())

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

		mutex.withLock {
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
	}

	open suspend fun replaceImagesOnDiskWithRemovedImagePlaceholder(photoName: String) {
		fun replaceFile(photoName: String, photoSuffix: String) {
			val path = "files\\photo_removed_image\\$photoRemovedPlaceHolderName${photoSuffix}.png"
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

			val smallPhotoPath = File("${ServerSettings.FILE_DIR_PATH}\\$photoName${photoSuffix}")
			placeholderFile.copyTo(smallPhotoPath, true)
		}

		mutex.withLock {
			//replace small photo with placeholder
			replaceFile(photoName, ServerSettings.SMALL_PHOTO_SUFFIX)

			//replace medium photo with placeholder
			replaceFile(photoName, ServerSettings.MEDIUM_PHOTO_SUFFIX)

			//replace big photo with placeholder
			replaceFile(photoName, ServerSettings.BIG_PHOTO_SUFFIX)

			//replace very big photo with placeholder
			replaceFile(photoName, ServerSettings.VERY_BIG_PHOTO_SUFFIX)
		}
	}

}