package com.kirakishou.photoexchange.util

import net.coobird.thumbnailator.Thumbnails
import java.awt.Dimension
import java.io.File
import javax.imageio.ImageIO

object ImageUtils {
	fun resizeAndSaveImageOnDisk(file: File, newMaxSize: Dimension, sizeType: String, currentFolderDirPath: String, imageNewName: String) {
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
}