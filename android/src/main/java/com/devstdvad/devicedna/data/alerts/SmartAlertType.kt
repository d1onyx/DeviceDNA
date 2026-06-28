package com.devstdvad.devicedna.data.alerts

import com.devstdvad.devicedna.R

/**
 * The critical conditions Smart Alerts watches for. [key] is persisted (per-alert toggle and
 * cooldown state); [route] is the in-app screen opened when the notification is tapped.
 */
enum class SmartAlertType(
    val key: String,
    val titleRes: Int,
    val bodyRes: Int,
    val settingRes: Int,
    val route: String,
) {
    CpuOverheating(
        key = "cpu_overheating",
        titleRes = R.string.alert_cpu_overheating_title,
        bodyRes = R.string.alert_cpu_overheating_body,
        settingRes = R.string.settings_alert_cpu_overheating,
        route = "hardware",
    ),
    LowBattery(
        key = "low_battery",
        titleRes = R.string.alert_low_battery_title,
        bodyRes = R.string.alert_low_battery_body,
        settingRes = R.string.settings_alert_low_battery,
        route = "hardware",
    ),
    StorageFull(
        key = "storage_full",
        titleRes = R.string.alert_storage_full_title,
        bodyRes = R.string.alert_storage_full_body,
        settingRes = R.string.settings_alert_storage_full,
        route = "system",
    ),
    HighRam(
        key = "high_ram",
        titleRes = R.string.alert_high_ram_title,
        bodyRes = R.string.alert_high_ram_body,
        settingRes = R.string.settings_alert_high_ram,
        route = "system",
    ),
    SlowCharging(
        key = "slow_charging",
        titleRes = R.string.alert_slow_charging_title,
        bodyRes = R.string.alert_slow_charging_body,
        settingRes = R.string.settings_alert_slow_charging,
        route = "hardware",
    ),
    ;

    companion object {
        fun fromKey(key: String): SmartAlertType? = entries.firstOrNull { it.key == key }
    }
}
