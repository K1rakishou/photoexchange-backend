package com.kirakishou.photoexchange.config

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.config.ServerSettings.DatabaseInfo.DB_NAME
import com.kirakishou.photoexchange.config.ServerSettings.DatabaseInfo.HOST
import com.kirakishou.photoexchange.config.ServerSettings.DatabaseInfo.PORT
import com.kirakishou.photoexchange.database.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoDao
import com.kirakishou.photoexchange.database.dao.PhotoInfoExchangeDao
import com.kirakishou.photoexchange.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoExchangeRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.handlers.GetGalleryPhotosHandler
import com.kirakishou.photoexchange.handlers.GetPhotoAnswerHandler
import com.kirakishou.photoexchange.handlers.GetPhotoHandler
import com.kirakishou.photoexchange.handlers.UploadPhotoHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.ConcurrencyService
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

fun myBeans() = beans {
	//router
	bean<Router>()

	bean { GsonBuilder().create() }
	bean { JsonConverterService(ref()) }
	bean { MongoRepositoryFactory(ref()) }
	bean { MongoTemplate(SimpleMongoDbFactory(MongoClient(HOST, PORT), DB_NAME)) }
	bean<ConcurrencyService>()

	//dao
	bean { MongoSequenceDao(ref()).also { it.init() } }
	bean { PhotoInfoDao(ref()).also { it.init() } }
	bean { PhotoInfoExchangeDao(ref()).also { it.init() } }
	bean { GalleryPhotoDao(ref()).also { it.init() } }

	//repository
	bean<PhotoInfoRepository>()
	bean<PhotoInfoExchangeRepository>()
	bean<GalleryPhotosRepository>()

	//service
	bean { GeneratorServiceImpl() }

	//handler
	bean<UploadPhotoHandler>()
	bean<GetPhotoAnswerHandler>()
	bean<GetPhotoHandler>()
	bean<GetGalleryPhotosHandler>()

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