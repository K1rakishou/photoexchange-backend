package com.kirakishou.photoexchange.config

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.config.ServerSettings.DatabaseInfo.DB_NAME
import com.kirakishou.photoexchange.config.ServerSettings.DatabaseInfo.HOST
import com.kirakishou.photoexchange.config.ServerSettings.DatabaseInfo.PORT
import com.kirakishou.photoexchange.database.mongo.dao.BanListDao
import com.kirakishou.photoexchange.database.mongo.dao.GalleryPhotoDao
import com.kirakishou.photoexchange.database.mongo.dao.MongoSequenceDao
import com.kirakishou.photoexchange.database.mongo.dao.ReportedPhotoDao
import com.kirakishou.photoexchange.database.mongo.repository.AdminInfoRepository
import com.kirakishou.photoexchange.database.mongo.repository.BanListRepository
import com.kirakishou.photoexchange.database.pgsql.repository.LocationMapRepository
import com.kirakishou.photoexchange.database.pgsql.repository.PhotosRepository
import com.kirakishou.photoexchange.database.pgsql.repository.UsersRepository
import com.kirakishou.photoexchange.handlers.*
import com.kirakishou.photoexchange.handlers.admin.BanPhotoHandler
import com.kirakishou.photoexchange.handlers.admin.BanUserAndAllTheirPhotosHandler
import com.kirakishou.photoexchange.handlers.admin.BanUserHandler
import com.kirakishou.photoexchange.handlers.admin.StartCleanupHandler
import com.kirakishou.photoexchange.handlers.count.GetFreshGalleryPhotosCountHandler
import com.kirakishou.photoexchange.handlers.count.GetFreshReceivedPhotosCountHandler
import com.kirakishou.photoexchange.handlers.count.GetFreshUploadedPhotosCountHandler
import com.kirakishou.photoexchange.routers.Router
import com.kirakishou.photoexchange.service.*
import com.samskivert.mustache.Mustache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import org.springframework.boot.autoconfigure.mustache.MustacheResourceTemplateLoader
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver
import org.springframework.context.support.beans
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.HandlerStrategies
import org.springframework.web.reactive.function.server.RouterFunctions

fun myBeans(adminToken: String) = beans {
  //router
  bean<Router>()

  bean { WebClient.builder().build() }
  bean { GsonBuilder().excludeFieldsWithoutExposeAnnotation().create() }
  bean { ReactiveMongoRepositoryFactory(ref()) }
  bean { ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(ConnectionString("mongodb://$HOST:$PORT/$DB_NAME"))) }

  //dao
  bean { AdminInfoRepository(adminToken) }
  bean { MongoSequenceDao(ref()).also { it.create() } }
  bean { PhotoInfoDao(ref()).also { it.create() } }
  bean { GalleryPhotoDao(ref()).also { it.create() } }
  bean { FavouritedPhotoDao(ref()).also { it.create() } }
  bean { ReportedPhotoDao(ref()).also { it.create() } }
  bean { UserInfoDao(ref()).also { it.create() } }
  bean { LocationMapDao(ref()).also { it.create() } }
  bean { BanListDao(ref()).also { it.create() } }

  //repository
  bean {
    PhotosRepository(
      ref(),
      ref(),
      ref(),
      ref(),
      ref(),
      ref(),
      ref(),
      ref(),
      ref(),
      Dispatchers.IO
    )
  }
  bean {
    UsersRepository(ref(), ref(), ref(), Dispatchers.IO)
  }
  bean {
    LocationMapRepository(
      ref(),
      ref(),
      ref(),
      ref(),
      Dispatchers.IO
    )
  }
  bean {
    BanListRepository(ref(), ref(), Dispatchers.IO)
  }

  //service
  bean {
    StaticMapDownloaderService(ref(), ref(), ref(), ref(), newFixedThreadPoolContext(4, "map-downloader"))
      .also { it.init() }
  }
  bean {
    PushNotificationSenderService(ref(), ref(), ref(), ref(), ref(), newFixedThreadPoolContext(4, "push-sender"))
  }
  bean {
    GoogleCredentialsService(newFixedThreadPoolContext(1, "google-token-refresher"))
  }

  bean { JsonConverterService(ref()) }
  bean<GeneratorService>()
  bean<RemoteAddressExtractorService>()
  bean<DiskManipulationService>()
  bean<WebClientService>()
  bean {
    CleanupService(
      ref(),
      ServerSettings.OLD_PHOTOS_CLEANUP_ROUTINE_INTERVAL,
      ServerSettings.UPLOADED_OLDER_THAN_TIME_DELTA,
      ServerSettings.DELETED_EARLIER_THAN_TIME_DELTA
    ).also { GlobalScope.launch { it.startCleaningRoutine() } }
  }

  //handler
  bean<UploadPhotoHandler>()
  bean<ReceivePhotosHandler>()
  bean<GetPhotoHandler>()
  bean<GetGalleryPhotosHandler>()
  bean<FavouritePhotoHandler>()
  bean<ReportPhotoHandler>()
  bean<GetUserIdHandler>()
  bean<GetUploadedPhotosHandler>()
  bean<GetReceivedPhotosHandler>()
  bean<GetStaticMapHandler>()
  bean<CheckAccountExistsHandler>()
  bean<UpdateFirebaseTokenHandler>()
  bean<GetFreshGalleryPhotosCountHandler>()
  bean<GetFreshUploadedPhotosCountHandler>()
  bean<GetFreshReceivedPhotosCountHandler>()
  bean<BanPhotoHandler>()
  bean<BanUserHandler>()
  bean<StartCleanupHandler>()
  bean<GetPhotosAdditionalInfoHandler>()
  bean<BanUserAndAllTheirPhotosHandler>()

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