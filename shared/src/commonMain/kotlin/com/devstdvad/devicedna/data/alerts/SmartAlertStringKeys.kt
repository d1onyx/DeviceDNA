package com.devstdvad.devicedna.data.alerts

/**
 * Maps each [SmartAlertType] to its i18n string key (resolved via `stringRes(...)`).
 * Shared so both the settings UI and notification builders use identical copy across platforms.
 */
val SmartAlertType.titleKey: String
    get() = when (this) {
        SmartAlertType.CpuOverheating -> "alert_cpu_overheating_title"
        SmartAlertType.LowBattery -> "alert_low_battery_title"
        SmartAlertType.StorageFull -> "alert_storage_full_title"
        SmartAlertType.HighRam -> "alert_high_ram_title"
        SmartAlertType.SlowCharging -> "alert_slow_charging_title"
    }

val SmartAlertType.bodyKey: String
    get() = when (this) {
        SmartAlertType.CpuOverheating -> "alert_cpu_overheating_body"
        SmartAlertType.LowBattery -> "alert_low_battery_body"
        SmartAlertType.StorageFull -> "alert_storage_full_body"
        SmartAlertType.HighRam -> "alert_high_ram_body"
        SmartAlertType.SlowCharging -> "alert_slow_charging_body"
    }

val SmartAlertType.settingKey: String
    get() = when (this) {
        SmartAlertType.CpuOverheating -> "settings_alert_cpu_overheating"
        SmartAlertType.LowBattery -> "settings_alert_low_battery"
        SmartAlertType.StorageFull -> "settings_alert_storage_full"
        SmartAlertType.HighRam -> "settings_alert_high_ram"
        SmartAlertType.SlowCharging -> "settings_alert_slow_charging"
    }
