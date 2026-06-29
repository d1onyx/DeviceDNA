package com.devstdvad.devicedna.data.sync

import com.devstdvad.devicedna.data.sync.model.DeviceSyncPayload
import com.devstdvad.devicedna.data.sync.model.DeviceSyncStatus
import com.devstdvad.devicedna.data.sync.model.GooglePlaySubscriptionVerificationPayload
import com.devstdvad.devicedna.data.sync.model.SubscriptionViewResponse
import com.devstdvad.devicedna.data.sync.model.SyncResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

enum class AccountStatus {
    Exists,
    NotFound,
    Disabled,
}

@Serializable
private data class BackendErrorResponse(
    val error: String? = null,
)

/** Thin wrapper over the sync backend. [baseUrl] must have no trailing slash. */
class SyncApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    suspend fun getAccountStatus(idToken: String): AccountStatus {
        val response: HttpResponse = client.get("$baseUrl/v1/me") {
            bearerAuth(idToken)
        }

        return when (response.status) {
            HttpStatusCode.OK -> AccountStatus.Exists
            HttpStatusCode.Forbidden -> {
                val error = response.errorCode()
                if (error == "account_disabled") {
                    AccountStatus.Disabled
                } else {
                    error("Account check failed: $error.")
                }
            }
            HttpStatusCode.NotFound,
            HttpStatusCode.Unauthorized -> AccountStatus.NotFound
            else -> error("Account check failed with HTTP ${response.status.value}.")
        }
    }

    suspend fun getStatus(idToken: String, androidId: String): DeviceSyncStatus =
        client.get("$baseUrl/v1/devices/$androidId/status") {
            bearerAuth(idToken)
        }.body()

    suspend fun postSync(idToken: String, payload: DeviceSyncPayload): SyncResult =
        client.post("$baseUrl/v1/sync") {
            bearerAuth(idToken)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()

    suspend fun verifyGooglePlaySubscription(
        idToken: String,
        payload: GooglePlaySubscriptionVerificationPayload,
    ): SubscriptionViewResponse =
        client.post("$baseUrl/v1/subscription/google-play/verify") {
            bearerAuth(idToken)
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()

    suspend fun activateDevSubscription(idToken: String): SubscriptionViewResponse =
        client.post("$baseUrl/v1/subscription/dev/activate") {
            bearerAuth(idToken)
        }.body()
}

private suspend fun HttpResponse.errorCode(): String =
    runCatching { body<BackendErrorResponse>().error }.getOrNull() ?: "unknown_error"
