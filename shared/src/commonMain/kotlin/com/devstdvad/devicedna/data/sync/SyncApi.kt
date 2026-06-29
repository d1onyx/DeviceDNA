package com.devstdvad.devicedna.data.sync

import com.devstdvad.devicedna.data.sync.model.DeviceSyncPayload
import com.devstdvad.devicedna.data.sync.model.DeviceSyncStatus
import com.devstdvad.devicedna.data.sync.model.SyncResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/** Thin wrapper over the sync backend. [baseUrl] must have no trailing slash. */
class SyncApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
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
}
