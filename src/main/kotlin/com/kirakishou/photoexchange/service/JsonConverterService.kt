package com.kirakishou.photoexchange.service

import com.google.gson.Gson
import core.SharedConstants
import org.springframework.core.io.buffer.DataBuffer

open class JsonConverterService(
  val gson: Gson
) {

  @Suppress("UNCHECKED_CAST")
  @Throws(PacketSizeExceeded::class)
  inline fun <reified T> fromJson(
    dataBufferList: List<DataBuffer>,
    maxSize: Long = SharedConstants.MAX_PACKET_SIZE
  ): T {
    return gson.fromJson(
      dataBufferToString(dataBufferList, maxSize),
      T::class.java
    ) as T
  }

  open fun <T> toJson(data: T): String {
    return gson.toJson(data)
  }

  @Throws(PacketSizeExceeded::class)
  fun dataBufferToString(
    dataBufferList: List<DataBuffer>,
    maxSize: Long
  ): String {
    val fullLength = dataBufferList.sumBy { it.readableByteCount() }
    if (fullLength > maxSize) {
      throw PacketSizeExceeded()
    }

    val array = ByteArray(fullLength)
    var offset = 0

    for (dataBuffer in dataBufferList) {
      val arrayLength = dataBuffer.readableByteCount()
      dataBuffer.read(array, offset, arrayLength)
      offset += arrayLength
    }

    return String(array)
  }

  class PacketSizeExceeded : Exception()
}