package com.kirakishou.photoexchange.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.handlers.*
import com.kirakishou.photoexchange.repository.MongoSequenceRepository
import com.kirakishou.photoexchange.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.GeneratorServiceImpl
import com.kirakishou.photoexchange.service.JsonConverterService
import com.mongodb.MongoClient
import com.samskivert.mustache.Mustache
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.springframework.boot.autoconfigure.mustache.MustacheResourceTemplateLoader
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver
import org.springframework.context.support.beans
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunctions

fun myBeans(dbInfo: DatabaseInfo) = beans {
    bean<Router>()
    bean<UploadPhotoHandler>()
    bean<GetPhotoAnswerHandler>()
    bean<GetPhotoHandler>()
    bean<MarkPhotoAsReceivedHandler>()
    bean<GetUserLocationHandler>()
    bean("mongoPool") {
        newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors(), "mongo")
    }
    bean<Gson> {
        GsonBuilder().create()
    }
    bean {
        JsonConverterService(ref())
    }
    bean {
        MongoSequenceRepository(ref())
    }
    bean {
        PhotoInfoRepository(ref(), ref(), ref("mongoPool"))
    }
    bean {
        GeneratorServiceImpl()
    }
    bean {
        MongoRepositoryFactory(ref())
    }
    bean {
        MongoTemplate(SimpleMongoDbFactory(MongoClient(dbInfo.host, dbInfo.port), dbInfo.dbName))
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

class DatabaseInfo(
        val host: String,
        val port: Int,
        val dbName: String
)