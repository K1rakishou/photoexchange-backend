package com.kirakishou.photoexchange.util

import net.coobird.thumbnailator.Thumbnails
import java.awt.Dimension
import java.io.File
import javax.imageio.ImageIO

object ImageUtils {

    @Throws(Exception::class)
    fun resizeAndSaveImageOnDisk(file: File, newMaxSize: Dimension, sizeType: String, currentFolderDirPath: String, imageNewName: String) {
        val imageToResize = ImageIO.read(file)
        val outputResizedFile = File("$currentFolderDirPath\\$imageNewName$sizeType")

        if (imageToResize == null) {
            println("imageToResize is null!!!")
        }

        //original image size should be bigger than the new size, otherwise we don't need to resize image, just copy it
        if (imageToResize.width > newMaxSize.width || imageToResize.height > newMaxSize.height) {
            val resizedImage = Thumbnails.of(imageToResize)
                    .useExifOrientation(true)
                    .size(newMaxSize.width, newMaxSize.height)
                    .asBufferedImage()

            ImageIO.write(resizedImage, "JPG", outputResizedFile)

        } else {
            file.copyTo(outputResizedFile)
        }
    }
}