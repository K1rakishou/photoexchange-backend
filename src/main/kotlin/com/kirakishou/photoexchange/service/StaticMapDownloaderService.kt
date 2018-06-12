package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.config.ServerSettings
import com.kirakishou.photoexchange.config.ServerSettings.GOOGLE_MAPS_KEY
import com.kirakishou.photoexchange.database.repository.LocationMapRepository
import com.kirakishou.photoexchange.database.repository.PhotoInfoRepository
import com.kirakishou.photoexchange.extensions.deleteIfExists
import com.kirakishou.photoexchange.model.repo.LocationMap
import com.kirakishou.photoexchange.service.concurrency.AbstractConcurrencyService
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.reactive.awaitFirstOrNull
import kotlinx.coroutines.experimental.reactive.awaitLast
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import java.io.File
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

open class StaticMapDownloaderService(
	private val photoInfoRepository: PhotoInfoRepository,
	private val locationMapRepository: LocationMapRepository,
	private val concurrencyService: AbstractConcurrencyService
) {
	private val logger = LoggerFactory.getLogger(StaticMapDownloaderService::class.java)
	private val client = WebClient.builder().build()
	private val requestStringFormat = "https://maps.googleapis.com/maps/api/staticmap?" +
		"center=%9.7f,%9.7f&" +
		"markers=color:red|%9.7f,%9.7f&" +
		"zoom=10&" +
		"size=600x600&" +
		"maptype=roadmap&" +
		"key=$GOOGLE_MAPS_KEY"
	private val REQUESTS_PER_BATCH = 100
	private val MAX_TIMEOUT_SECONDS = 15L
	private val CHUNCKS_COUNT = 4
	private val inProgress = AtomicBoolean(false)

	fun init() {
		//check if there are not downloaded map files on every server start
		startDownloadingMapFiles()
	}

	open suspend fun enqueue(photoId: Long): Boolean {
		val result = locationMapRepository.save(LocationMap.create(photoId))

		//start the downloading process regardless of the save result because there might be old requests in the queue
		//and we want to process them
		startDownloadingMapFiles()

		return !result.isEmpty()
	}

	private fun startDownloadingMapFiles() {
		if (!inProgress.compareAndSet(false, true)) {
			return
		}

		concurrencyService.asyncCommon {
			try {
				val requestsBatch = locationMapRepository.getOldest(REQUESTS_PER_BATCH)
				if (requestsBatch.isNotEmpty()) {
					processBatch(requestsBatch.chunked(CHUNCKS_COUNT))
				}
			} finally {
				inProgress.set(false)
			}
		}
	}

	private suspend fun processBatch(requestsBatchChunked: List<List<LocationMap>>) {
		for (chunk in requestsBatchChunked) {
			val resultList = mutableListOf<Deferred<Boolean>>()

			//send concurrent requests (CHUNCKS_COUNT max) to the google servers
			for (locationMap in chunk) {
				resultList += concurrencyService.asyncMap { getLocationMap(locationMap) }
			}

			//all errors are being handled inside of the coroutine so we shouldn't get any exceptions here
			resultList.forEach { it.await() }
		}
	}

	private suspend fun getLocationMap(locationMap: LocationMap): Boolean {
		try {
			val photoInfo = photoInfoRepository.findOneById(locationMap.photoId)
			if (photoInfo.isEmpty()) {
				return false
			}

			val lon = photoInfo.lon
			val lat = photoInfo.lat
			val photoMapName = "${photoInfo.photoName}_map"
			val requestString = String.format(requestStringFormat, lon, lat, lon, lat)

			logger.debug("Trying to get map from google services with request string = $requestString")

			val response = client.get()
				.uri(requestString)
				.exchange()
				.timeout(Duration.ofSeconds(MAX_TIMEOUT_SECONDS))
				.awaitFirstOrNull()

			if (response == null) {
				logger.error("Response is null")
				return false
			}

			if (!response.statusCode().is2xxSuccessful) {
				logger.error("Response status code is not 2xxSuccessful (${response.statusCode()})")
				return false
			}

			val filePath = "${ServerSettings.FILE_DIR_PATH}\\$photoMapName"
			val outFile = File(filePath)

			try {
				if (!saveFileToDisk(outFile, response)) {
					logger.error("Could not save file to disk")
					outFile.deleteIfExists()
					return false
				}

				if (!locationMapRepository.setMapReady(locationMap.photoId)) {
					logger.error("Could not set mapReady flag in the DB")
					outFile.deleteIfExists()
				}
			} catch (error: Throwable) {
				outFile.deleteIfExists()
			}

		} catch (error: Throwable) {
			logger.error("Unknown error", error)
		}

		return true
	}

	private suspend fun saveFileToDisk(outFile: File, response: ClientResponse): Boolean {
		try {
			outFile.outputStream().use { outputStream ->
				response.body(BodyExtractors.toDataBuffers())
					.doOnNext { chunk ->
						chunk.asInputStream().use { inputStream ->
							val chunkSize = inputStream.available()
							val buffer = ByteArray(chunkSize)

							//copy chunks from one stream to another
							inputStream.read(buffer, 0, chunkSize)
							outputStream.write(buffer, 0, chunkSize)
						}
					}.awaitLast()
			}

		} catch (error: Throwable) {
			outFile.deleteIfExists()
			return false
		}

		return true
	}
}