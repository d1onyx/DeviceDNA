package com.devstdvad.devicedna.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.sync.DeviceSyncManager
import com.devstdvad.devicedna.data.sync.SyncOutcome
import com.devstdvad.devicedna.data.sync.SyncStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUiState(
    val isSyncing: Boolean = false,
    val lastOutcome: SyncOutcome? = null,
    val lastSyncTime: Long = 0L,
)

class SyncViewModel(
    private val manager: DeviceSyncManager,
    private val stateStore: SyncStateStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    private var autoTriggered = false

    init {
        viewModelScope.launch {
            _state.update { it.copy(lastSyncTime = stateStore.current().lastSyncTime) }
        }
    }

    /** Called on startup — syncs only once per ViewModel lifetime. */
    fun triggerOnce() {
        if (autoTriggered) return
        autoTriggered = true
        sync(force = false)
    }

    fun sync(force: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isSyncing = true) }
            val outcome = manager.syncIfNeeded(force)
            _state.update {
                it.copy(
                    isSyncing = false,
                    lastOutcome = outcome,
                    lastSyncTime = stateStore.current().lastSyncTime,
                )
            }
        }
    }
}
