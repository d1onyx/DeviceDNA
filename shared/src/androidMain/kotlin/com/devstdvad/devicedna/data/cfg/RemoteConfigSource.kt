package com.devstdvad.devicedna.data.cfg

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class Payload(@SerialName("isUnlocked") val active: Boolean)

@OptIn(ExperimentalEncodingApi::class)
class RemoteConfigSource(
    private val documentPath: String,
    private val check: SignatureCheck,
    private val appName: String? = null,
    private val payloadField: String = "payload",
    private val sigField: String = "sig",
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val document
        get() = (appName?.let { Firebase.firestore(Firebase.app(it)) } ?: Firebase.firestore)
            .document(documentPath)

    fun updates(): Flow<RemoteState> = document.snapshots.map(::evaluate)

    private fun evaluate(snapshot: DocumentSnapshot): RemoteState {
        if (!snapshot.exists || !snapshot.contains(payloadField) || !snapshot.contains(sigField)) {
            return RemoteState(null)
        }
        return evaluate(snapshot.get<String>(payloadField), snapshot.get<String>(sigField))
    }

    internal fun evaluate(payload: String?, sig: String?): RemoteState {
        if (payload == null || sig == null) return RemoteState(null)
        val payloadBytes = runCatching { Base64.decode(payload) }.getOrNull() ?: return RemoteState(null)
        val sigBytes = runCatching { Base64.decode(sig) }.getOrNull() ?: return RemoteState(null)
        if (!check.verify(payloadBytes, sigBytes)) return RemoteState(null)
        val parsed = runCatching { json.decodeFromString<Payload>(payloadBytes.decodeToString()) }
            .getOrNull() ?: return RemoteState(null)
        return RemoteState(parsed.active)
    }
}
