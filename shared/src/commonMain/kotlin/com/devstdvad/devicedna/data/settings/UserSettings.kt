package com.devstdvad.devicedna.data.settings

import com.devstdvad.devicedna.data.alerts.SmartAlertType

val ALL_SMART_ALERT_KEYS: Set<String> = SmartAlertType.entries.map { it.key }.toSet()

data class UserSettings(
    val maskSensitive: Boolean = true,
    val reducedMotion: Boolean = false,
    val fastRefresh: Boolean = false,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.Celsius,
    val dataUnit: DataUnit = DataUnit.GB,
    val theme: AppThemeMode = AppThemeMode.System,
    val publicIpEnabled: Boolean = false,
    val showImei: Boolean = false,
    val backgroundMonitoring: Boolean = false,
    val onboardingComplete: Boolean = false,
    val appLanguage: String = "",
    val hapticFeedback: Boolean = true,
    val soundEffects: Boolean = false,
    val exportFormat: ExportFormat = ExportFormat.Json,
    val widgetsPromoShown: Boolean = false,
    val smartAlertsEnabled: Boolean = true,
    val smartAlertTypes: Set<String> = ALL_SMART_ALERT_KEYS,
)

enum class TemperatureUnit { Celsius, Fahrenheit }
enum class DataUnit { GB, GiB }
enum class AppThemeMode { Light, Dark, System }
enum class ExportFormat { Json, Csv, Txt }
