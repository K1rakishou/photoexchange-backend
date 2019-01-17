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

object ApplicationConfig {
  const val COROUTINE_SCHEDULER_GENERAL_NAME = "general"
  const val COROUTINE_SCHEDULER_IO_NAME = "io"
  const val COROUTINE_SCHEDULER_MAP_DOWNLOADER_NAME = "map-downloader"
  const val COROUTINE_SCHEDULER_PUSH_SENDER_NAME = "push-sender"
  const val COROUTINE_SCHEDULER_GOOGLE_TOKEN_REFRESHER_NAME = "google-token-refresher"


  fun initBeans(adminToken: String) = beans {
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
    bean(COROUTINE_SCHEDULER_GENERAL_NAME) {
      val threadsCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
      newFixedThreadPoolContext(threadsCount, COROUTINE_SCHEDULER_GENERAL_NAME)
    }
    bean(COROUTINE_SCHEDULER_IO_NAME) {
      Dispatchers.IO
    }
    bean(COROUTINE_SCHEDULER_MAP_DOWNLOADER_NAME) {
      val threadsCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
      newFixedThreadPoolContext(threadsCount, COROUTINE_SCHEDULER_MAP_DOWNLOADER_NAME)
    }
    bean(COROUTINE_SCHEDULER_PUSH_SENDER_NAME) {
      val threadsCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
      newFixedThreadPoolContext(threadsCount, COROUTINE_SCHEDULER_PUSH_SENDER_NAME)
    }
    bean(COROUTINE_SCHEDULER_GOOGLE_TOKEN_REFRESHER_NAME) {
      newFixedThreadPoolContext(1, COROUTINE_SCHEDULER_GOOGLE_TOKEN_REFRESHER_NAME)
    }

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
        ref<DatabaseFactory>().db,
        ref(COROUTINE_SCHEDULER_IO_NAME)
      )
    }
    bean {
      UsersRepository(ref(), ref(), ref<DatabaseFactory>().db, ref(COROUTINE_SCHEDULER_IO_NAME))
    }
    bean {
      LocationMapRepository(ref(), ref(), ref<DatabaseFactory>().db, ref(COROUTINE_SCHEDULER_IO_NAME))
    }
    bean {
      BanListRepository(ref(), ref<DatabaseFactory>().db, ref(COROUTINE_SCHEDULER_IO_NAME))
    }

    //service
    bean {
      StaticMapDownloaderService(
        ref(),
        ref(),
        ref(),
        ref(),
        ref(COROUTINE_SCHEDULER_MAP_DOWNLOADER_NAME)
      ).also { it.init() }
    }
    bean {
      PushNotificationSenderService(ref(), ref(), ref(), ref(), ref(), ref(COROUTINE_SCHEDULER_PUSH_SENDER_NAME))
    }
    bean {
      GoogleCredentialsService(ref(COROUTINE_SCHEDULER_GOOGLE_TOKEN_REFRESHER_NAME))
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
    bean {
      UploadPhotoHandler(
        ref(),
        ref(),
        ref(),
        ref(),
        ref(),
        ref(),
        ref(),
        ref(),
        ref(COROUTINE_SCHEDULER_GENERAL_NAME),
        ref()
      )
    }
    bean {
      ReceivePhotosHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetPhotoHandler(ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetGalleryPhotosHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      FavouritePhotoHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      ReportPhotoHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetUserUuidHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetUploadedPhotosHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetReceivedPhotosHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetStaticMapHandler(ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      CheckAccountExistsHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      UpdateFirebaseTokenHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetFreshGalleryPhotosCountHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetFreshUploadedPhotosCountHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetFreshReceivedPhotosCountHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      BanPhotoHandler(ref(), ref(), ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      BanUserHandler(ref(), ref(), ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      StartCleanupHandler(ref(), ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      GetPhotosAdditionalInfoHandler(ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }
    bean {
      BanUserAndAllTheirPhotosHandler(ref(), ref(), ref(), ref(), ref(COROUTINE_SCHEDULER_GENERAL_NAME), ref())
    }

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

}