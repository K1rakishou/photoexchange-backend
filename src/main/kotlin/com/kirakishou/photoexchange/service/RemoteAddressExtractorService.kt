package com.kirakishou.photoexchange.service

import org.springframework.web.reactive.function.server.ServerRequest
import java.lang.IllegalArgumentException
import java.net.Inet4Address
import java.net.Inet6Address

open class RemoteAddressExtractorService {

  open fun extractRemoteAddress(serverRequest: ServerRequest): String {
    if (!serverRequest.remoteAddress().isPresent) {
      throw RuntimeException("Request does not contain remote address!")
    }

    val inetAddress = serverRequest.remoteAddress().get().address

    return when (inetAddress) {
      is Inet4Address -> inetAddress.hostAddress
      is Inet6Address -> inetAddress.hostAddress
      else -> throw IllegalArgumentException("Unknown type of inetAddress: ${inetAddress::class}")
    }
  }
}