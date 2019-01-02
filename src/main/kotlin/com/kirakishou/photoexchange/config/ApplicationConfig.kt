package com.kirakishou.photoexchange.config

import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.*
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
  bean<DatabaseFactory>()

  //dao
  bean { AdminInfoRepository(adminToken) }
  bean<PhotosDao>()
  bean<GalleryPhotosDao>()
  bean<FavouritedPhotosDao>()
  bean<ReportedPhotosDao>()
  bean<UsersDao>()
  bean<LocationMapsDao>()
  bean<BansDao>()

  //dispatchers
  bean("IO") {
    Dispatchers.IO
  }
  bean("map-downloader") {
    newFixedThreadPoolContext(4, "map-downloader")
  }
  bean("push-sender") {
    newFixedThreadPoolContext(4, "push-sender")
  }
  bean("google-token-refresher") {
    newFixedThreadPoolContext(1, "google-token-refresher")
  }

  //repository
  bean {
    PhotosRepository(ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref(), ref<DatabaseFactory>().db, ref("IO"))
  }
  bean {
    UsersRepository(ref(), ref(), ref<DatabaseFactory>().db, ref("IO"))
  }
  bean {
    LocationMapRepository(ref(), ref(), ref<DatabaseFactory>().db, ref("IO"))
  }
  bean {
    BanListRepository(ref(), ref<DatabaseFactory>().db, ref("IO"))
  }

  //service
  bean {
    StaticMapDownloaderService(ref(), ref(), ref(), ref(), ref("map-downloader")).also { it.init() }
  }
  bean {
    PushNotificationSenderService(ref(), ref(), ref(), ref(), ref(), ref("push-sender"))
  }
  bean {
    GoogleCredentialsService(ref("google-token-refresher"))
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
  bean<GetUserUuidHandler>()
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
  bean("webHandler") {
    RouterFunctions.toWebHandler(
      ref<Router>().setUpRouter(),
      HandlerStrategies.builder().viewResolver(ref()).build()
    )
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