package com.devstdvad.devicedna.data.cfg

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

fun interface SignatureCheck {
    fun verify(message: ByteArray, signature: ByteArray): Boolean
}

class SyncConfig(
    val enabled: Boolean,
    val documentPath: String,
    val appName: String,
    val publicKeyBase64: String,
)

data class RemoteState(val active: Boolean?)

class ConfigSync private constructor(
    private val store: ConfigStore?,
    private val updates: (() -> Flow<RemoteState>)?,
    private val unavailableState: Boolean,
    private val startupRefreshWindowMs: Long,
    @Suppress("UNUSED_PARAMETER") marker: Unit,
) {
    constructor() : this(null, null, false, 0L, Unit)

    internal constructor(
        store: ConfigStore?,
        updates: (() -> Flow<RemoteState>)?,
        unavailableState: Boolean = false,
        startupRefreshWindowMs: Long = 0L,
    ) : this(store, updates, unavailableState, startupRefreshWindowMs, Unit)

    private val _degraded = MutableStateFlow(unavailableState)

    val degraded: StateFlow<Boolean> = _degraded.asStateFlow()

    private var listenerJob: Job? = null
    private var startupLockJob: Job? = null
    private var pendingStartupLock = false

    fun onStartup() {
        val cached = store?.degraded ?: unavailableState
        pendingStartupLock = cached && startupRefreshWindowMs > 0L
        _degraded.value = if (pendingStartupLock) false else cached
    }

    fun attach(scope: CoroutineScope) {
        val updates = updates ?: return
        listenerJob?.cancel()
        listenerJob = updates().onEach(::onState).launchIn(scope)
        if (pendingStartupLock && startupLockJob == null) {
            startupLockJob = scope.launch {
                delay(startupRefreshWindowMs)
                if (pendingStartupLock && store?.degraded == true) {
                    _degraded.value = true
                }
                pendingStartupLock = false
                startupLockJob = null
            }
        }
    }

    fun detach() {
        listenerJob?.cancel()
        listenerJob = null
    }

    private fun onState(state: RemoteState) {
        val store = store ?: return
        when (state.active) {
            true -> {
                pendingStartupLock = false
                startupLockJob?.cancel()
                startupLockJob = null
                store.degraded = false
                _degraded.value = false
            }
            false -> {
                pendingStartupLock = false
                startupLockJob?.cancel()
                startupLockJob = null
                store.degraded = true
                _degraded.value = true
            }
            null -> Unit
        }
    }
}
