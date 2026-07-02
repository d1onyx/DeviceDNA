package com.devstdvad.devicedna.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * iOS-side factory that wires the Ktor Darwin engine into [SyncApi]. Building the
 * HttpClient with its engine must happen in Kotlin/Native (iosMain), so Swift just
 * calls `IosSyncFactory.shared.create(baseUrl:)` and gets a ready [SyncApi].
 */
object IosSyncFactory {
    fun create(baseUrl: String): SyncApi {
        val client = HttpClient(Darwin) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    }
                )
            }
        }
        return SyncApi(client, baseUrl)
    }
}
