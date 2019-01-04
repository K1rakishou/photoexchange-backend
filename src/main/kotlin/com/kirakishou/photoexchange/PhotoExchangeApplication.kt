package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.config.ApplicationConfig
import org.springframework.context.support.GenericApplicationContext
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.netty.http.server.HttpServer
import java.time.Duration

class PhotoExchangeApplication(
	private val args: Array<String>,
	private val port: Int = 8080
) {

	fun startAndAwait() {
    //first argument should be a random string that you will use as a token to do admin stuff
    //See: Router.kt hasAuthHeaderPredicate() method

    val adminToken = args[0]
    if (adminToken.length < 48) {
      throw RuntimeException("Admin token should have length of at least 48 symbols. Current passed token length is ${adminToken.length}")
    }

    val context = GenericApplicationContext().apply {
      ApplicationConfig.initBeans(adminToken).initialize(this)
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
	PhotoExchangeApplication(args).startAndAwait()
}