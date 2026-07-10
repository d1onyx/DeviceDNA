package com.devstdvad.devicedna.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

/**
 * iOS [SettingsStore] backed by NSUserDefaults. A MutableStateFlow mirrors the persisted
 * state so shared ViewModels get live updates, matching the Android DataStore behaviour.
 */
class IosSettingsStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : SettingsStore {

    private object Key {
        const val MASK_SENSITIVE = "mask_sensitive"
        const val REDUCED_MOTION = "reduced_motion"
        const val FAST_REFRESH = "fast_refresh"
        const val TEMPERATURE_UNIT = "temperature_unit"
        const val DATA_UNIT = "data_unit"
        const val THEME = "theme"
        const val PUBLIC_IP_ENABLED = "public_ip_enabled"
        const val SHOW_IMEI = "show_imei"
        const val BACKGROUND_MONITORING = "background_monitoring"
        const val ONBOARDING_COMPLETE = "onboarding_complete"
        const val APP_LANGUAGE = "app_language"
        const val HAPTIC_FEEDBACK = "haptic_feedback"
        const val SOUND_EFFECTS = "sound_effects"
        const val EXPORT_FORMAT = "export_format"
        const val WIDGETS_PROMO_SHOWN = "widgets_promo_shown"
        const val SMART_ALERTS_ENABLED = "smart_alerts_enabled"
        const val SMART_ALERT_TYPES = "smart_alert_types"
    }

    private val state = MutableStateFlow(load())

    override val settings: Flow<UserSettings> = state

    private fun boolOr(key: String, fallback: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) fallback else defaults.boolForKey(key)

    private inline fun <reified T : Enum<T>> stringEnum(key: String, default: T): T {
        val raw = defaults.stringForKey(key) ?: return default
        return enumValues<T>().firstOrNull { it.name == raw } ?: default
    }

    private fun load(): UserSettings {
        @Suppress("UNCHECKED_CAST")
        val alertTypes = (defaults.arrayForKey(Key.SMART_ALERT_TYPES) as? List<String>)?.toSet()
        return UserSettings(
            maskSensitive = boolOr(Key.MASK_SENSITIVE, true),
            reducedMotion = boolOr(Key.REDUCED_MOTION, false),
            fastRefresh = boolOr(Key.FAST_REFRESH, false),
            temperatureUnit = stringEnum(Key.TEMPERATURE_UNIT, TemperatureUnit.Celsius),
            dataUnit = stringEnum(Key.DATA_UNIT, DataUnit.GB),
            theme = stringEnum(Key.THEME, AppThemeMode.System),
            publicIpEnabled = boolOr(Key.PUBLIC_IP_ENABLED, false),
            showImei = boolOr(Key.SHOW_IMEI, false),
            backgroundMonitoring = boolOr(Key.BACKGROUND_MONITORING, false),
            onboardingComplete = boolOr(Key.ONBOARDING_COMPLETE, false),
            appLanguage = defaults.stringForKey(Key.APP_LANGUAGE) ?: "",
            hapticFeedback = boolOr(Key.HAPTIC_FEEDBACK, true),
            soundEffects = boolOr(Key.SOUND_EFFECTS, false),
            exportFormat = stringEnum(Key.EXPORT_FORMAT, ExportFormat.Json),
            widgetsPromoShown = boolOr(Key.WIDGETS_PROMO_SHOWN, false),
            smartAlertsEnabled = boolOr(Key.SMART_ALERTS_ENABLED, true),
            smartAlertTypes = alertTypes ?: ALL_SMART_ALERT_KEYS,
        )
    }

    private fun setBool(key: String, value: Boolean) {
        defaults.setBool(value, key)
        state.value = load()
    }

    private fun setString(key: String, value: String) {
        defaults.setObject(value, key)
        state.value = load()
    }

    override suspend fun setMaskSensitive(value: Boolean) = setBool(Key.MASK_SENSITIVE, value)
    override suspend fun setReducedMotion(value: Boolean) = setBool(Key.REDUCED_MOTION, value)
    override suspend fun setFastRefresh(value: Boolean) = setBool(Key.FAST_REFRESH, value)
    override suspend fun setTemperatureUnit(value: TemperatureUnit) = setString(Key.TEMPERATURE_UNIT, value.name)
    override suspend fun setDataUnit(value: DataUnit) = setString(Key.DATA_UNIT, value.name)
    override suspend fun setTheme(value: AppThemeMode) = setString(Key.THEME, value.name)
    override suspend fun setPublicIpEnabled(value: Boolean) = setBool(Key.PUBLIC_IP_ENABLED, value)
    override suspend fun setShowImei(value: Boolean) = setBool(Key.SHOW_IMEI, value)
    override suspend fun setBackgroundMonitoring(value: Boolean) = setBool(Key.BACKGROUND_MONITORING, value)
    override suspend fun setOnboardingComplete(value: Boolean) = setBool(Key.ONBOARDING_COMPLETE, value)
    override suspend fun setAppLanguage(value: String) = setString(Key.APP_LANGUAGE, value)
    override suspend fun setHapticFeedback(value: Boolean) = setBool(Key.HAPTIC_FEEDBACK, value)
    override suspend fun setSoundEffects(value: Boolean) = setBool(Key.SOUND_EFFECTS, value)
    override suspend fun setExportFormat(value: ExportFormat) = setString(Key.EXPORT_FORMAT, value.name)
    override suspend fun setWidgetsPromoShown(value: Boolean) = setBool(Key.WIDGETS_PROMO_SHOWN, value)
    override suspend fun setSmartAlertsEnabled(value: Boolean) = setBool(Key.SMART_ALERTS_ENABLED, value)

    override suspend fun setSmartAlertTypeEnabled(typeKey: String, enabled: Boolean) {
        val current = state.value.smartAlertTypes
        val next = if (enabled) current + typeKey else current - typeKey
        defaults.setObject(next.toList(), Key.SMART_ALERT_TYPES)
        state.value = load()
    }

    override suspend fun clear() {
        ALL_KEYS.forEach { defaults.removeObjectForKey(it) }
        state.value = load()
    }

    private companion object {
        val ALL_KEYS = listOf(
            Key.MASK_SENSITIVE,
            Key.REDUCED_MOTION,
            Key.FAST_REFRESH,
            Key.TEMPERATURE_UNIT,
            Key.DATA_UNIT,
            Key.THEME,
            Key.PUBLIC_IP_ENABLED,
            Key.SHOW_IMEI,
            Key.BACKGROUND_MONITORING,
            Key.ONBOARDING_COMPLETE,
            Key.APP_LANGUAGE,
            Key.HAPTIC_FEEDBACK,
            Key.SOUND_EFFECTS,
            Key.EXPORT_FORMAT,
            Key.WIDGETS_PROMO_SHOWN,
            Key.SMART_ALERTS_ENABLED,
            Key.SMART_ALERT_TYPES,
        )
    }
}
