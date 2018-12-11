package com.kirakishou.photoexchange.exception

class DatabaseTransactionException(msg: String) : Exception(msg)
class EmptyPacket : Exception()
class ExchangeException : Exception()

class CouldNotUpdateMapReadyFlag(message: String) : Exception(message)
class CouldNotUpdateMapAnonymousFlag(message: String) : Exception(message)
class CouldNotUpdateLocationId(message: String) : Exception(message)