package com.kirakishou.photoexchange.config

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.config.Settings.MONGO_POOL_BEAN_NAME
import com.kirakishou.photoexchange.config.Settings.MONGO_POOL_NAME
import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.database.repository.PhotoInfoExchangeRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.*
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
	//router
	bean<Router>()

	bean { GsonBuilder().create() }
	bean { JsonConverterService(ref()) }
	bean { MongoRepositoryFactory(ref()) }
	bean { MongoTemplate(SimpleMongoDbFactory(MongoClient(dbInfo.host, dbInfo.port), dbInfo.dbName)) }

	//handler
	bean<UploadPhotoHandler>()
	bean<GetPhotoAnswerHandler>()
	bean<GetPhotoHandler>()
	bean<MarkPhotoAsReceivedHandler>()
	bean<GetUserLocationHandler>()

	//thread pool
	bean(MONGO_POOL_BEAN_NAME) { newFixedThreadPoolContext(Runtime.getRuntime().availableProcessors(), MONGO_POOL_NAME) }

	//dao
	bean { MongoSequenceDao(ref()).also { it.init() } }
	bean { PhotoInfoDao(ref()).also { it.init() } }
	bean { PhotoInfoExchangeDao(ref()).also { it.init() } }

	//repository
	bean { PhotoInfoRepository(ref(), ref(), ref(), ref(MONGO_POOL_BEAN_NAME)) }
	bean { PhotoInfoExchangeRepository(ref(), ref(), ref(MONGO_POOL_BEAN_NAME)) }

	//service
	bean { GeneratorServiceImpl() }

	//etc
	bean("webHandler") { RouterFunctions.toWebHandler(ref<Router>().setUpRouter(), HandlerStrategies.builder().viewResolver(ref()).build()) }
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

object Settings {
	const val MONGO_POOL_NAME = "mongo"
	const val MONGO_POOL_BEAN_NAME = "mongoPool"
}