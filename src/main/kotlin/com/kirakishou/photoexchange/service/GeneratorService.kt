package com.kirakishou.photoexchange.service

import java.security.SecureRandom

class GeneratorService {
	private val numericAlphabetic = "0123456789abcdefghijklmnopqrstuvwxyz"
	private val random = SecureRandom()

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

	fun generateNewPhotoName(): String {
		return generateRandomString(32, numericAlphabetic)
	}

	fun generateUserId(): String {
		return generateRandomString(20, numericAlphabetic) + "@photoexchange.io"
	}
}