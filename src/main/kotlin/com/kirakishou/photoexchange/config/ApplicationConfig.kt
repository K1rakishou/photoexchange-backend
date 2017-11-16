package com.kirakishou.photoexchange.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.handlers.GetPhotoAnswerHandler
import com.kirakishou.photoexchange.handlers.GetPhotoHandler
import com.kirakishou.photoexchange.handlers.MarkPhotoAsReceivedHandler
import com.kirakishou.photoexchange.handlers.UploadPhotoHandler
import com.kirakishou.photoexchange.repository.MongoSequenceRepository
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.GeneratorServiceImpl
import com.kirakishou.photoexchange.service.JsonConverterService
import com.mongodb.MongoClient
import com.samskivert.mustache.Mustache
import org.springframework.boot.autoconfigure.mustache.MustacheResourceTemplateLoader
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver
import org.springframework.context.support.beans
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunctions

const val DB_SERVER_ADDRESS = "192.168.99.100:27017"

fun myBeans() = beans {
    bean<Router>()
    bean<UploadPhotoHandler>()
    bean<GetPhotoAnswerHandler>()
    bean<GetPhotoHandler>()
    bean<MarkPhotoAsReceivedHandler>()
    bean<Gson> {
        GsonBuilder().create()
    }
    bean<JsonConverterService> {
        JsonConverterService(ref())
    }
    bean {
        MongoSequenceRepository(ref())
    }
    bean {
        PhotoInfoRepository(ref(), ref())
    }
    bean {
        GeneratorServiceImpl()
    }
    bean {
        MongoRepositoryFactory(ref())
    }
    bean {
        MongoTemplate(SimpleMongoDbFactory(MongoClient("192.168.99.100", 27017), "photoexhange"))
    }
    bean("webHandler") {
        RouterFunctions.toWebHandler(ref<Router>().setUpRouter(), HandlerStrategies.builder().viewResolver(ref()).build())
    }
    bean {
        val prefix = "classpath:/templates/"
        val suffix = ".mustache"
        val loader = MustacheResourceTemplateLoader(prefix, suffix)
        MustacheViewResolver(Mustache.compiler().withLoader(loader)).apply {
            setPrefix(prefix)
            setSuffix(suffix)
        }
    }
}