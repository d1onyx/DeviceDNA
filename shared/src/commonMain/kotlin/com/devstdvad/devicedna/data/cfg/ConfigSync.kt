package com.devstdvad.devicedna.data.cfg

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Verifies a signature over a message with an embedded public key. */
fun interface SignatureCheck {
    fun verify(message: ByteArray, signature: ByteArray): Boolean
}

/** Remote config parameters supplied per platform (Android BuildConfig, iOS Info.plist). */
class SyncConfig(
    val enabled: Boolean,
    val documentPath: String,
    val appName: String,
    val publicKeyBase64: String,
)

/**
 * Observes a remote config document and exposes [degraded] as reactive state.
 *
 * Reacts only to a validly-signed payload; a missing document or untrusted/unsigned data is
 * ignored and the last known state is kept. The state is cached so it persists across restarts and
 * offline. Inert (always false) when [store]/[source] are null.
 */
class ConfigSync internal constructor(
    private val store: ConfigStore?,
    private val source: RemoteConfigSource?,
) {
    private val _degraded = MutableStateFlow(false)

    val degraded: StateFlow<Boolean> = _degraded.asStateFlow()

    private var listenerJob: Job? = null

    /** Seed [degraded] from the persisted state (it survives offline/restart). */
    fun onStartup() {
        _degraded.value = store?.degraded ?: false
    }

    /** Subscribe to realtime updates (foreground). Safe to call repeatedly. */
    fun attach(scope: CoroutineScope) {
        val source = source ?: return
        listenerJob?.cancel()
        listenerJob = source.updates().onEach(::onState).launchIn(scope)
    }

    fun detach() {
        listenerJob?.cancel()
        listenerJob = null
    }

    private fun onState(state: RemoteState) {
        val store = store ?: return
        when (state.active) {
            true -> {
                store.degraded = false
                _degraded.value = false
            }
            false -> {
                store.degraded = true
                _degraded.value = true
            }
            null -> Unit // untrusted/missing → ignore, keep the current state
        }
    }
}

/** Assembles a [ConfigSync]; returns a disabled instance when [config] is off or no check exists. */
fun buildConfigSync(
    config: SyncConfig,
    settings: Settings,
    check: SignatureCheck?,
): ConfigSync {
    if (!config.enabled || check == null) return ConfigSync(store = null, source = null)
    val source = RemoteConfigSource(
        documentPath = config.documentPath,
        check = check,
        appName = config.appName,
    )
    val store: ConfigStore = SettingsConfigStore(settings)
    return ConfigSync(store = store, source = source)
}
