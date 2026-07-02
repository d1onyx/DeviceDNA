package com.devstdvad.devicedna.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.auth.AuthGateway
import com.devstdvad.devicedna.data.auth.AuthUser
import com.devstdvad.devicedna.data.settings.AppThemeMode
import com.devstdvad.devicedna.data.settings.DataUnit
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.data.settings.TemperatureUnit
import com.devstdvad.devicedna.data.settings.UserSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val authRepository: AuthGateway,
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
}
