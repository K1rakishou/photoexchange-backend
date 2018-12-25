package com.kirakishou.photoexchange.service

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import kotlin.coroutines.CoroutineContext

open class GoogleCredentialsService(
  private val dispatcher: CoroutineDispatcher
) : CoroutineScope {
  private val logger = LoggerFactory.getLogger(GoogleCredentialsService::class.java)
  private val job = Job()
  private val minTimeUntilRefresh = 30L

  private val googleCredential = GoogleCredential
    // Your service-account.json should be in the resources directory
    // (https://cloud.google.com/iam/docs/creating-managing-service-account-keys)
    .fromStream(ClassPathResource("service-account.json").inputStream)
    .createScoped(SCOPES)

  override val coroutineContext: CoroutineContext
    get() = job + dispatcher

  open suspend fun getAccessToken(): String {
    return withContext(coroutineContext) {
      try {
        // If expiresInSeconds is not null and accessToken is not null and we have 30 more seconds
        // before token expires - return old token otherwise - refresh token
        if (googleCredential.expiresInSeconds != null
          && googleCredential.accessToken != null
          && googleCredential.expiresInSeconds > minTimeUntilRefresh) {
          return@withContext googleCredential.accessToken
        }

        logger.debug("Refreshing token...")
        googleCredential.refreshToken()
        logger.debug("Done refreshing token...")

        return@withContext googleCredential.accessToken
      } catch (error: Throwable) {
        logger.error("Could not acquire access token", error)
        return@withContext ""
      }
    }
  }

  companion object {
    private val MESSAGING_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private val SCOPES = listOf(MESSAGING_SCOPE)
  }
}