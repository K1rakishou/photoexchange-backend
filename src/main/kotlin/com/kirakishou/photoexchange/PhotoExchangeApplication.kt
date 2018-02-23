package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.config.myBeans
import org.springframework.context.support.GenericApplicationContext
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.ipc.netty.http.server.HttpServer
import reactor.ipc.netty.tcp.BlockingNettyContext

class PhotoExchangeApplication(port: Int = 8080) {
	private val httpHandler: HttpHandler
	private val server: HttpServer
	private lateinit var nettyContext: BlockingNettyContext

	init {
		val context = GenericApplicationContext().apply {
			myBeans().initialize(this)
			refresh()
		}

		server = HttpServer.create(port)
		httpHandler = WebHttpHandlerBuilder.applicationContext(context).build()
	}

	fun start() {
		nettyContext = server.start(ReactorHttpHandlerAdapter(httpHandler))
	}

	fun startAndAwait() {
		server.startAndAwait(ReactorHttpHandlerAdapter(httpHandler), { nettyContext = it })
	}

	fun stop() {
		nettyContext.shutdown()
	}
}

fun main(args: Array<String>) {
	checkThreadPoolsPercentageCorrectness()

	PhotoExchangeApplication().startAndAwait()
}

private fun checkThreadPoolsPercentageCorrectness() {
	if (ServerSettings.ThreadPool.Mongo.MONGO_THREADS_PERCENTAGE +
		ServerSettings.ThreadPool.Common.COMMON_THREADS_PERCENTAGE > 1.0) {
		throw IllegalStateException("Total threads percentage cannot be bigger than 1.0")
	}
}