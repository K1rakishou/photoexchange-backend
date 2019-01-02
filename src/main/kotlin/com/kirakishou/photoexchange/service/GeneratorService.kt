package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.core.PhotoName
import com.kirakishou.photoexchange.core.UserUuid
import java.security.SecureRandom
import java.util.*

open class GeneratorService(
	private val random: Random = SecureRandom(),
	private val numericAlphabetic: String = "0123456789abcdefghijklmnopqrstuvwxyz"
) {

	fun generateRandomString(len: Int, alphabet: String): String {
		val bytes = ByteArray(len)
		random.nextBytes(bytes)

		val sb = StringBuilder()
		val alphabetLen = alphabet.length

		for (i in 0 until len) {
			sb.append(alphabet[Math.abs(bytes[i] % alphabetLen)])
		}

		return sb.toString()
	}

	fun generateNewPhotoName(): PhotoName {
		return PhotoName(generateRandomString(32, numericAlphabetic))
	}

	fun generateUserUuid(): UserUuid {
		return UserUuid(generateRandomString(20, numericAlphabetic) + "@photoexchange.io")
	}
}