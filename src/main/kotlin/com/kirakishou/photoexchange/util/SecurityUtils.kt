package com.kirakishou.photoexchange.util

import com.kirakishou.photoexchange.extensions.toHex
import java.security.MessageDigest
import java.util.*
import kotlin.streams.asSequence

object SecurityUtils {

  object Generation {
    val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    fun generateRandomString(length: Int): String {
      return Random().ints(length.toLong(), 0, source.length)
        .asSequence()
        .map(source::get)
        .joinToString("")
    }
  }

  object Hashing {
    private val sha3 = MessageDigest.getInstance("SHA-512")

    @Synchronized
    fun sha3(data: ByteArray): String {
      return sha3.digest(data).toHex().toUpperCase()
    }

    @Synchronized
    fun sha3(data: String): String {
      return sha3(data.toByteArray())
    }
  }
}