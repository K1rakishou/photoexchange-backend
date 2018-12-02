package com.kirakishou.photoexchange.util

import com.kirakishou.photoexchange.extensions.toHex
import java.security.MessageDigest

object SecurityUtils {
  object Hashing {
    private val sha3 = MessageDigest.getInstance("SHA-512")

    fun sha3(data: ByteArray): String {
      return sha3.digest(data).toHex().toUpperCase()
    }

    fun sha3(data: String): String {
      return sha3(data.toByteArray())
    }
  }
}