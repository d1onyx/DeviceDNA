package com.devstdvad.devicedna.presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.data.sync.AccountCheckOutcome
import com.devstdvad.devicedna.data.sync.DeviceSyncManager
import com.devstdvad.devicedna.data.sync.SyncOutcome
import com.devstdvad.devicedna.data.sync.SyncStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUiState(
    val accountCheckKey: String? = null,
    val isCheckingAccount: Boolean = false,
    val lastAccountCheck: AccountCheckOutcome? = null,
    val isSyncing: Boolean = false,
    val lastOutcome: SyncOutcome? = null,
    val lastSyncTime: Long = 0L,
)

class SyncViewModel(
    private val manager: DeviceSyncManager,
    private val stateStore: SyncStateStore,
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    private var lastAutoTriggeredAccountKey: String? = null
    private var checkedAccountKey: String? = null

    init {
        viewModelScope.launch {
            _state.update { it.copy(lastSyncTime = stateStore.current().lastSyncTime) }
        }
    }

    /** Called on startup before showing signed-in app content. */
    fun verifyAccountOnce(accountKey: String?) {
        if (accountKey.isNullOrBlank() || checkedAccountKey == accountKey) return
        checkedAccountKey = accountKey
        viewModelScope.launch {
            _state.update {
                it.copy(
                    accountCheckKey = accountKey,
                    isCheckingAccount = true,
                    lastAccountCheck = null,
                )
            }
            val outcome = manager.ensureAccountExists()
            _state.update { it.copy(isCheckingAccount = false, lastAccountCheck = outcome) }
        }
    }

    /**
     * Silent re-check for app resume: catches an account deleted/disabled on another device while
     * this one stayed signed in. Unlike [verifyAccountOnce] it never flips the UI to the loading
     * gate. It is deliberately one-way — only a conclusive Removed/Disabled overwrites the state
     * (dropping AppNavigation to the sign-in screen); a transient network Failed (e.g. resuming
     * offline) is ignored so a verified user is never bounced out by a blip. No-ops until the
     * initial check has run.
     */
    fun recheckAccount() {
        if (checkedAccountKey == null) return
        viewModelScope.launch {
            when (manager.ensureAccountExists()) {
                AccountCheckOutcome.Removed ->
                    _state.update { it.copy(lastAccountCheck = AccountCheckOutcome.Removed) }
                AccountCheckOutcome.Disabled ->
                    _state.update { it.copy(lastAccountCheck = AccountCheckOutcome.Disabled) }
                else -> Unit
            }
        }
    }

    /** Automatically syncs whenever the active account changes. */
    fun triggerOnce(accountKey: String?) {
        if (accountKey.isNullOrBlank() || lastAutoTriggeredAccountKey == accountKey) return
        lastAutoTriggeredAccountKey = accountKey
        sync(force = false)
        // Re-check entitlements against the backend on startup so a server-side change
        // (revoked/expired subscription) is reflected even before the user opens the premium screen.
        viewModelScope.launch {
            runCatching { subscriptionRepository.refreshEntitlements() }
        }
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
