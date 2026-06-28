package com.devstdvad.devicedna.data.alerts

import com.devstdvad.devicedna.data.widget.WidgetSnapshot

/**
 * Pure (testable) Smart Alerts logic: given a metrics snapshot and the set of enabled alert
 * types, returns the alerts whose condition is currently active. No Android dependencies.
 */
object SmartAlertEvaluator {

    const val CPU_TEMP_C = 45f
    const val THERMAL_MAX_C = 48f
    const val THERMAL_STATUS_SEVERE = 3
    const val LOW_BATTERY_PCT = 15
    const val STORAGE_FULL_FRACTION = 0.90f
    const val HIGH_RAM_FRACTION = 0.90f
    const val SLOW_CHARGE_WATTS = 5f
    const val SLOW_CHARGE_MAX_LEVEL = 80

    fun evaluate(snapshot: WidgetSnapshot, enabled: Set<SmartAlertType>): List<SmartAlertType> =
        SmartAlertType.entries.filter { it in enabled && isActive(it, snapshot) }

    fun isActive(type: SmartAlertType, s: WidgetSnapshot): Boolean = when (type) {
        SmartAlertType.CpuOverheating ->
            s.cpuTempC >= CPU_TEMP_C || s.thermalMaxC >= THERMAL_MAX_C || s.thermalStatus >= THERMAL_STATUS_SEVERE

        SmartAlertType.LowBattery ->
            s.batteryLevel in 1..LOW_BATTERY_PCT && !s.batteryCharging

        SmartAlertType.StorageFull ->
            s.storageUsedPercent >= STORAGE_FULL_FRACTION

        SmartAlertType.HighRam ->
            s.ramUsedPercent >= HIGH_RAM_FRACTION

        SmartAlertType.SlowCharging ->
            s.batteryCharging && s.batteryWatts in 0.1f..SLOW_CHARGE_WATTS && s.batteryLevel in 1 until SLOW_CHARGE_MAX_LEVEL
    }
}
