package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.config.DatabaseInfo
import com.kirakishou.photoexchange.config.myBeans
import org.slf4j.LoggerFactory
import org.springframework.context.support.GenericApplicationContext
import org.springframework.http.server.reactive.HttpHandler
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.ipc.netty.http.server.HttpServer
import reactor.ipc.netty.tcp.BlockingNettyContext

class PhotoExchangeApplication(port: Int = 8080) {
    private val logger = LoggerFactory.getLogger(PhotoExchangeApplication::class.java)
    private val httpHandler: HttpHandler
    private val server: HttpServer
    private lateinit var nettyContext: BlockingNettyContext

    init {
        val dbInfo = DatabaseInfo("192.168.99.100", 27017, "photoexhange")

        val context = GenericApplicationContext().apply {
            myBeans(dbInfo).initialize(this)
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
    PhotoExchangeApplication().startAndAwait()
}