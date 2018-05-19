package com.kirakishou.photoexchange.config

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.config.ServerSettings.DatabaseInfo.HOST
import com.kirakishou.photoexchange.config.ServerSettings.DatabaseInfo.PORT
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.GalleryPhotosRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoExchangeRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.database.repository.UserInfoRepository
import com.kirakishou.photoexchange.handlers.*
import com.kirakishou.photoexchange.handlers.gallery_photos.GetGalleryPhotoIdsHandler
import com.kirakishou.photoexchange.handlers.gallery_photos.GetGalleryPhotoInfoHandler
import com.kirakishou.photoexchange.handlers.gallery_photos.GetGalleryPhotosHandler
import com.kirakishou.photoexchange.handlers.received_photos.GetReceivedPhotoIdsHandler
import com.kirakishou.photoexchange.handlers.received_photos.GetReceivedPhotosHandler
import com.kirakishou.photoexchange.handlers.uploaded_photos.GetUploadedPhotoIdsHandler
import com.kirakishou.photoexchange.handlers.uploaded_photos.GetUploadedPhotosHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.GeneratorService
import com.kirakishou.photoexchange.service.JsonConverterService
import com.kirakishou.photoexchange.service.concurrency.ConcurrencyService
import com.mongodb.ConnectionString
import com.samskivert.mustache.Mustache
import org.springframework.boot.autoconfigure.mustache.MustacheResourceTemplateLoader
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver
import org.springframework.context.support.beans
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunctions

fun myBeans() = beans {
	//router
	bean<Router>()

	bean { GsonBuilder().create() }
	bean { JsonConverterService(ref()) }
	bean { MongoRepositoryFactory(ref()) }
	bean { ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(ConnectionString("mongodb://$HOST:$PORT/DB_NAME"))) }
	bean<ConcurrencyService>()

	//dao
	bean { MongoSequenceDao(ref()).also { it.create() } }
	bean { PhotoInfoDao(ref()).also { it.create() } }
	bean { PhotoInfoExchangeDao(ref()).also { it.create() } }
	bean { GalleryPhotoDao(ref()).also { it.create() } }
	bean { FavouritedPhotoDao(ref()).also { it.create() } }
	bean { ReportedPhotoDao(ref()).also { it.create() } }
	bean { UserInfoDao(ref()).also { it.create() } }

	//repository
	bean<PhotoInfoRepository>()
	bean<PhotoInfoExchangeRepository>()
	bean<GalleryPhotosRepository>()
	bean<UserInfoRepository>()

	//service
	bean { GeneratorService() }

	//handler
	bean<UploadPhotoHandler>()
	bean<ReceivePhotosHandler>()
	bean<GetPhotoHandler>()
	bean<GetGalleryPhotoIdsHandler>()
	bean<GetGalleryPhotosHandler>()
	bean<GetGalleryPhotoInfoHandler>()
	bean<FavouritePhotoHandler>()
	bean<ReportPhotoHandler>()
	bean<GetUserIdHandler>()
	bean<GetUploadedPhotoIdsHandler>()
	bean<GetUploadedPhotosHandler>()
	bean<GetReceivedPhotoIdsHandler>()
	bean<GetReceivedPhotosHandler>()

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