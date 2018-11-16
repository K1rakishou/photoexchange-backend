package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.config.myBeans
import org.springframework.context.support.GenericApplicationContext
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.netty.DisposableServer
import reactor.netty.http.server.HttpServer

class PhotoExchangeApplication(val port: Int = 8080) {
	private lateinit var server: DisposableServer
	private val httpHandler: HttpHandler

	init {
		val context = GenericApplicationContext().apply {
			myBeans().initialize(this)
			refresh()
		}

		httpHandler = WebHttpHandlerBuilder.applicationContext(context).build()
	}

	fun startAndAwait() {
		server = HttpServer.create()
			.port(port)
			.handle(ReactorHttpHandlerAdapter(httpHandler))
			.bind()
			.block()!!
	}

	fun stop() {
		server.disposeNow()
	}
}

fun main(args: Array<String>) {
	PhotoExchangeApplication().startAndAwait()
}