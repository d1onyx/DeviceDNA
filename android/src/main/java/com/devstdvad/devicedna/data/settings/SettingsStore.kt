package com.devstdvad.devicedna.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("device_dna_settings")

class SettingsStore(private val context: Context) {

    val settings: Flow<UserSettings> = context.settingsDataStore.data.map { prefs ->
        UserSettings(
            maskSensitive = prefs[MASK_SENSITIVE] ?: true,
            reducedMotion = prefs[REDUCED_MOTION] ?: false,
            fastRefresh = prefs[FAST_REFRESH] ?: false,
            temperatureUnit = prefs[TEMPERATURE_UNIT]?.toEnumOrDefault(TemperatureUnit.Celsius) ?: TemperatureUnit.Celsius,
            dataUnit = prefs[DATA_UNIT]?.toEnumOrDefault(DataUnit.GB) ?: DataUnit.GB,
            theme = prefs[THEME]?.toEnumOrDefault(AppThemeMode.System) ?: AppThemeMode.System,
            publicIpEnabled = prefs[PUBLIC_IP_ENABLED] ?: false,
            showImei = prefs[SHOW_IMEI] ?: false,
            backgroundMonitoring = prefs[BACKGROUND_MONITORING] ?: false,
            onboardingComplete = prefs[ONBOARDING_COMPLETE] ?: false,
            appLanguage = prefs[APP_LANGUAGE] ?: "",
            hapticFeedback = prefs[HAPTIC_FEEDBACK] ?: true,
            soundEffects = prefs[SOUND_EFFECTS] ?: false,
            exportFormat = prefs[EXPORT_FORMAT]?.toEnumOrDefault(ExportFormat.Json) ?: ExportFormat.Json,
            widgetsPromoShown = prefs[WIDGETS_PROMO_SHOWN] ?: false,
            smartAlertsEnabled = prefs[SMART_ALERTS_ENABLED] ?: true,
            smartAlertTypes = prefs[SMART_ALERT_TYPES] ?: ALL_SMART_ALERT_KEYS,
        )
    }

    suspend fun setMaskSensitive(value: Boolean) {
        context.settingsDataStore.edit { it[MASK_SENSITIVE] = value }
    }

    suspend fun setReducedMotion(value: Boolean) {
        context.settingsDataStore.edit { it[REDUCED_MOTION] = value }
    }

    suspend fun setFastRefresh(value: Boolean) {
        context.settingsDataStore.edit { it[FAST_REFRESH] = value }
    }

    suspend fun setTemperatureUnit(value: TemperatureUnit) {
        context.settingsDataStore.edit { it[TEMPERATURE_UNIT] = value.name }
    }

    suspend fun setDataUnit(value: DataUnit) {
        context.settingsDataStore.edit { it[DATA_UNIT] = value.name }
    }

    suspend fun setTheme(value: AppThemeMode) {
        context.settingsDataStore.edit { it[THEME] = value.name }
    }

    suspend fun setPublicIpEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[PUBLIC_IP_ENABLED] = value }
    }

    suspend fun setShowImei(value: Boolean) {
        context.settingsDataStore.edit { it[SHOW_IMEI] = value }
    }

    suspend fun setBackgroundMonitoring(value: Boolean) {
        context.settingsDataStore.edit { it[BACKGROUND_MONITORING] = value }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.settingsDataStore.edit { it[ONBOARDING_COMPLETE] = value }
    }

    suspend fun setAppLanguage(value: String) {
        context.settingsDataStore.edit { it[APP_LANGUAGE] = value }
    }

    suspend fun setHapticFeedback(value: Boolean) {
        context.settingsDataStore.edit { it[HAPTIC_FEEDBACK] = value }
    }

    suspend fun setSoundEffects(value: Boolean) {
        context.settingsDataStore.edit { it[SOUND_EFFECTS] = value }
    }

    suspend fun setExportFormat(value: ExportFormat) {
        context.settingsDataStore.edit { it[EXPORT_FORMAT] = value.name }
    }

    suspend fun setWidgetsPromoShown(value: Boolean) {
        context.settingsDataStore.edit { it[WIDGETS_PROMO_SHOWN] = value }
    }

    suspend fun setSmartAlertsEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[SMART_ALERTS_ENABLED] = value }
    }

    suspend fun setSmartAlertTypeEnabled(typeKey: String, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            val current = prefs[SMART_ALERT_TYPES] ?: ALL_SMART_ALERT_KEYS
            prefs[SMART_ALERT_TYPES] = if (enabled) current + typeKey else current - typeKey
        }
    }

    private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T =
        enumValues<T>().firstOrNull { it.name == this } ?: default

    private companion object {
        val MASK_SENSITIVE = booleanPreferencesKey("mask_sensitive")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val FAST_REFRESH = booleanPreferencesKey("fast_refresh")
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
        val DATA_UNIT = stringPreferencesKey("data_unit")
        val THEME = stringPreferencesKey("theme")
        val PUBLIC_IP_ENABLED = booleanPreferencesKey("public_ip_enabled")
        val SHOW_IMEI = booleanPreferencesKey("show_imei")
        val BACKGROUND_MONITORING = booleanPreferencesKey("background_monitoring")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")
        val WIDGETS_PROMO_SHOWN = booleanPreferencesKey("widgets_promo_shown")
        val SMART_ALERTS_ENABLED = booleanPreferencesKey("smart_alerts_enabled")
        val SMART_ALERT_TYPES = stringSetPreferencesKey("smart_alert_types")
    }
}
