package com.devstdvad.devicedna.data.settings

import com.devstdvad.devicedna.data.account.ClearableStore
import kotlinx.coroutines.flow.Flow

/**
 * Persistent user settings, platform-agnostic. Implemented per platform:
 *   • Android → AndroidSettingsStore (Jetpack DataStore)
 *   • iOS     → IosSettingsStore (NSUserDefaults), later
 * Lets shared ViewModels observe/mutate [UserSettings] without touching platform storage.
 */
interface SettingsStore : ClearableStore {
    val settings: Flow<UserSettings>

    suspend fun setMaskSensitive(value: Boolean)
    suspend fun setReducedMotion(value: Boolean)
    suspend fun setFastRefresh(value: Boolean)
    suspend fun setTemperatureUnit(value: TemperatureUnit)
    suspend fun setDataUnit(value: DataUnit)
    suspend fun setTheme(value: AppThemeMode)
    suspend fun setPublicIpEnabled(value: Boolean)
    suspend fun setShowImei(value: Boolean)
    suspend fun setBackgroundMonitoring(value: Boolean)
    suspend fun setOnboardingComplete(value: Boolean)
    suspend fun setAppLanguage(value: String)
    suspend fun setHapticFeedback(value: Boolean)
    suspend fun setSoundEffects(value: Boolean)
    suspend fun setExportFormat(value: ExportFormat)
    suspend fun setWidgetsPromoShown(value: Boolean)
    suspend fun setSmartAlertsEnabled(value: Boolean)
    suspend fun setSmartAlertTypeEnabled(typeKey: String, enabled: Boolean)
    suspend fun setGuestMode(value: Boolean)
}
