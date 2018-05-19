package com.kirakishou.photoexchange.repository

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.database.dao.*
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.model.repo.PhotoInfo
import com.kirakishou.photoexchange.service.GeneratorService
import com.kirakishou.photoexchange.service.concurrency.TestConcurrencyService
import com.mongodb.MongoClient
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoDbFactory
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner::class)
class PhotoInfoRepositoryTest {

//	@InjectMocks
//	lateinit var photoInfoRepository: PhotoInfoRepository
//
//	@Spy
//	lateinit var template: MongoTemplate
//	@Spy
//	lateinit var mongoSequenceDao: MongoSequenceDao
//	@Spy
//	lateinit var photoInfoDao: PhotoInfoDao
//	@Spy
//	lateinit var photoInfoExchangeDao: PhotoInfoExchangeDao
//	@Spy
//	lateinit var galleryPhotoDao: GalleryPhotoDao
//	@Spy
//	lateinit var favouritedPhotoDao: FavouritedPhotoDao
//	@Spy
//	lateinit var reportedPhotoDao: ReportedPhotoDao
//	@Spy
//	lateinit var userInfoDao: UserInfoDao
//
//	@Before
//	fun setUp() {
//		MockitoAnnotations.initMocks(this)
//	}

	@Test
	fun `test try do exchange`() {
		runBlocking {
			val template = MongoTemplate(SimpleMongoDbFactory(MongoClient(ServerSettings.DatabaseInfo.HOST, ServerSettings.DatabaseInfo.PORT),
				"photoexchange_test"))

			val mongoSequenceDao = MongoSequenceDao(template).also {
				it.clear()
				it.create()
			}
			val photoInfoDao = PhotoInfoDao(template).also {
				it.clear()
				it.create()
			}
			val photoInfoExchangeDao = PhotoInfoExchangeDao(template).also {
				it.clear()
				it.create()
			}
			val galleryPhotoDao = GalleryPhotoDao(template).also {
				it.clear()
				it.create()
			}
			val favouritedPhotoDao = FavouritedPhotoDao(template).also {
				it.clear()
				it.create()
			}
			val reportedPhotoDao = ReportedPhotoDao(template).also {
				it.clear()
				it.create()
			}
			val userInfoDao = UserInfoDao(template).also {
				it.clear()
				it.create()
			}

			val generator = GeneratorService()
			val concurrentService = TestConcurrencyService()

			val photoInfoRepository = PhotoInfoRepository(
				mongoSequenceDao,
				photoInfoDao,
				photoInfoExchangeDao,
				galleryPhotoDao,
				favouritedPhotoDao,
				reportedPhotoDao,
				userInfoDao,
				generator,
				concurrentService)

			Mockito.`when`(photoInfoRepository.findMany(Mockito.anyString(), Mockito.anyList()))
				.thenReturn(listOf(PhotoInfo.empty(), PhotoInfo.empty()))

			val result = photoInfoRepository.findMany("1", listOf("1", "2"))

			println()
			println()
			println()
			println()
			println()
			println()
		}
	}
}