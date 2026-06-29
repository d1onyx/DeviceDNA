package com.devstdvad.devicedna.data.alerts

import androidx.annotation.StringRes
import com.devstdvad.devicedna.R

val SmartAlertType.titleRes: Int
    @StringRes get() = when (this) {
        SmartAlertType.CpuOverheating -> R.string.alert_cpu_overheating_title
        SmartAlertType.LowBattery -> R.string.alert_low_battery_title
        SmartAlertType.StorageFull -> R.string.alert_storage_full_title
        SmartAlertType.HighRam -> R.string.alert_high_ram_title
        SmartAlertType.SlowCharging -> R.string.alert_slow_charging_title
    }

val SmartAlertType.bodyRes: Int
    @StringRes get() = when (this) {
        SmartAlertType.CpuOverheating -> R.string.alert_cpu_overheating_body
        SmartAlertType.LowBattery -> R.string.alert_low_battery_body
        SmartAlertType.StorageFull -> R.string.alert_storage_full_body
        SmartAlertType.HighRam -> R.string.alert_high_ram_body
        SmartAlertType.SlowCharging -> R.string.alert_slow_charging_body
    }

val SmartAlertType.settingRes: Int
    @StringRes get() = when (this) {
        SmartAlertType.CpuOverheating -> R.string.settings_alert_cpu_overheating
        SmartAlertType.LowBattery -> R.string.settings_alert_low_battery
        SmartAlertType.StorageFull -> R.string.settings_alert_storage_full
        SmartAlertType.HighRam -> R.string.settings_alert_high_ram
        SmartAlertType.SlowCharging -> R.string.settings_alert_slow_charging
    }
