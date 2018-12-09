package com.kirakishou.photoexchange.core

import java.io.File


open class FileWrapper(
  private val file: File? = null
) {

  fun isEmpty(): Boolean = file == null
  fun getFile() = file

  fun deleteIfExists(){
    file?.let {
      if (it.exists()) {
        it.delete()
      }
    }
  }
}