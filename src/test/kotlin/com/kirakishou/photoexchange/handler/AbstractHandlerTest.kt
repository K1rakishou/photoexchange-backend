package com.kirakishou.photoexchange.handler

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.model.net.response.UploadPhotoResponse
import org.springframework.test.web.reactive.server.WebTestClient

abstract class AbstractHandlerTest {
	val EPSILON = 0.00001
	val gson = GsonBuilder().create()

	inline fun <reified T> fromBodyContent(content: WebTestClient.BodyContentSpec): T {
		return gson.fromJson<UploadPhotoResponse>(String(content.returnResult().responseBody), UploadPhotoResponse::class.java) as T
	}
}