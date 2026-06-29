package com.devstdvad.devicedna.data.alerts

/**
 * Critical conditions watched by Smart Alerts. [key] is persisted; [route] is the in-app
 * screen opened by platform notification handlers.
 */
enum class SmartAlertType(
    val key: String,
    val route: String,
) {
    CpuOverheating(
        key = "cpu_overheating",
        route = "hardware",
    ),
    LowBattery(
        key = "low_battery",
        route = "hardware",
    ),
    StorageFull(
        key = "storage_full",
        route = "system",
    ),
    HighRam(
        key = "high_ram",
        route = "system",
    ),
    SlowCharging(
        key = "slow_charging",
        route = "hardware",
    ),
    ;

    companion object {
        fun fromKey(key: String): SmartAlertType? = entries.firstOrNull { it.key == key }
    }
}
