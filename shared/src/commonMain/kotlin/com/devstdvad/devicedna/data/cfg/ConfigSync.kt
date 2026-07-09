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
 * Blocks ONLY when the document carries a validly-signed lock (`isUnlocked = false`). With no
 * document, an unlock, or untrusted/unsigned data, the app runs — there is no time-based blocking
 * (no expiry, no offline window). A written lock is cached so it survives offline/restart until an
 * online unlock arrives. Disabled (always false) when [store]/[source] are null.
 */
class ConfigSync internal constructor(
    private val store: ConfigStore?,
    private val source: RemoteConfigSource?,
) {
    private val _degraded = MutableStateFlow(false)

    val degraded: StateFlow<Boolean> = _degraded.asStateFlow()

    private var listenerJob: Job? = null

    /** Seed [degraded] from the persisted state (a previously-written lock survives offline/restart). */
    fun onStartup() {
        _degraded.value = store?.blocked ?: false
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
                store.blocked = false
                _degraded.value = false
            }
            false -> {
                store.blocked = true
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
