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
 * offline.
 *
 * With no [store]/[source] the instance never observes anything and reports [unavailableState]
 * forever: `false` to fail open (config sync simply off), `true` to fail closed (the shell stays in
 * its warm-up phase, i.e. the app is unusable without the config app).
 */
class ConfigSync internal constructor(
    private val store: ConfigStore?,
    private val source: RemoteConfigSource?,
    private val unavailableState: Boolean = false,
) {
    private val _degraded = MutableStateFlow(unavailableState)

    val degraded: StateFlow<Boolean> = _degraded.asStateFlow()

    private var listenerJob: Job? = null

    /** Seed [degraded] from the persisted state (it survives offline/restart). */
    fun onStartup() {
        _degraded.value = store?.degraded ?: unavailableState
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

/**
 * Assembles a [ConfigSync]; returns a disabled instance when [config] is off or no check exists.
 *
 * [lockWhenUnavailable] decides what a disabled instance reports: `false` leaves the app fully
 * usable, `true` keeps it in its warm-up phase — use it where the config app is mandatory and a
 * stripped-out plist must not silently disable the switch.
 */
fun buildConfigSync(
    config: SyncConfig,
    settings: Settings,
    check: SignatureCheck?,
    lockWhenUnavailable: Boolean = false,
): ConfigSync {
    if (!config.enabled || check == null) {
        return ConfigSync(store = null, source = null, unavailableState = lockWhenUnavailable)
    }
    val source = RemoteConfigSource(
        documentPath = config.documentPath,
        check = check,
        appName = config.appName,
    )
    val store: ConfigStore = SettingsConfigStore(settings)
    return ConfigSync(store = store, source = source)
}
