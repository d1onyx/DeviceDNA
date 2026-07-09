package com.devstdvad.devicedna.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.auth.AccountDeletionResult
import com.devstdvad.devicedna.data.auth.AuthGateway
import com.devstdvad.devicedna.data.auth.AuthUser
import com.devstdvad.devicedna.data.settings.AppThemeMode
import com.devstdvad.devicedna.data.settings.DataUnit
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.data.settings.TemperatureUnit
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.data.sync.DeviceSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val authRepository: AuthGateway,
    private val syncManager: DeviceSyncManager,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings(),
    )

    val user: StateFlow<AuthUser?> = authRepository.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun setMaskSensitive(value: Boolean) {
        viewModelScope.launch { settingsStore.setMaskSensitive(value) }
    }

    fun setReducedMotion(value: Boolean) {
        viewModelScope.launch { settingsStore.setReducedMotion(value) }
    }

    fun setFastRefresh(value: Boolean) {
        viewModelScope.launch { settingsStore.setFastRefresh(value) }
    }

    fun setTemperatureUnit(value: TemperatureUnit) {
        viewModelScope.launch { settingsStore.setTemperatureUnit(value) }
    }

    fun setDataUnit(value: DataUnit) {
        viewModelScope.launch { settingsStore.setDataUnit(value) }
    }

    fun setTheme(value: AppThemeMode) {
        viewModelScope.launch { settingsStore.setTheme(value) }
    }

    fun setPublicIpEnabled(value: Boolean) {
        viewModelScope.launch { settingsStore.setPublicIpEnabled(value) }
    }

    fun setShowImei(value: Boolean) {
        viewModelScope.launch { settingsStore.setShowImei(value) }
    }

    fun setBackgroundMonitoring(value: Boolean) {
        viewModelScope.launch { settingsStore.setBackgroundMonitoring(value) }
    }

    fun setAppLanguage(value: String) {
        viewModelScope.launch { settingsStore.setAppLanguage(value) }
    }

    fun setHapticFeedback(value: Boolean) {
        viewModelScope.launch { settingsStore.setHapticFeedback(value) }
    }

    fun setSoundEffects(value: Boolean) {
        viewModelScope.launch { settingsStore.setSoundEffects(value) }
    }

    fun setExportFormat(value: ExportFormat) {
        viewModelScope.launch { settingsStore.setExportFormat(value) }
    }

    fun setSmartAlertsEnabled(value: Boolean) {
        viewModelScope.launch { settingsStore.setSmartAlertsEnabled(value) }
    }

    fun setSmartAlertTypeEnabled(typeKey: String, enabled: Boolean) {
        viewModelScope.launch { settingsStore.setSmartAlertTypeEnabled(typeKey, enabled) }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    private val _accountDeletion = MutableStateFlow(AccountDeletionUi.Idle)

    /** Drives the delete-account button/dialog. On success the auth listener returns to sign-in. */
    val accountDeletion: StateFlow<AccountDeletionUi> = _accountDeletion.asStateFlow()

    fun deleteAccount() {
        if (_accountDeletion.value == AccountDeletionUi.Deleting) return
        _accountDeletion.value = AccountDeletionUi.Deleting
        viewModelScope.launch {
            // Purge server-side data first, while still authenticated. The backend also cancels
            // any active Google Play subscription before deleting the account row.
            if (!syncManager.deleteAccountData()) {
                _accountDeletion.value = AccountDeletionUi.Failed
                return@launch
            }

            _accountDeletion.value = when (authRepository.deleteAccount()) {
                AccountDeletionResult.Deleted -> AccountDeletionUi.Idle
                AccountDeletionResult.ReauthRequired -> AccountDeletionUi.ReauthRequired
                AccountDeletionResult.Failed -> AccountDeletionUi.Failed
            }
        }
    }

    fun dismissDeletionError() {
        if (_accountDeletion.value != AccountDeletionUi.Deleting) {
            _accountDeletion.value = AccountDeletionUi.Idle
        }
    }
}

enum class AccountDeletionUi { Idle, Deleting, ReauthRequired, Failed }
