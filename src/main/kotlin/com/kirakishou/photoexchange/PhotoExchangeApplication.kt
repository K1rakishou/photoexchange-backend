package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.config.myBeans
import org.springframework.context.support.GenericApplicationContext
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.netty.http.server.HttpServer
import java.time.Duration

class PhotoExchangeApplication(val port: Int = 8080) {

	fun startAndAwait() {
    val context = GenericApplicationContext().apply {
      myBeans().initialize(this)
      refresh()
    }

    val httpHandler = WebHttpHandlerBuilder.applicationContext(context).build()

		HttpServer.create()
			.host("0.0.0.0")
			.port(port)
			.handle(ReactorHttpHandlerAdapter(httpHandler))
			.bindUntilJavaShutdown(Duration.ofSeconds(10), null)
	}
}

fun main(args: Array<String>) {
	PhotoExchangeApplication().startAndAwait()
}