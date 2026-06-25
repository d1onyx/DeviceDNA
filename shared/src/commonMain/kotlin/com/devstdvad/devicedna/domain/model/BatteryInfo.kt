package com.devstdvad.devicedna.domain.model

data class BatteryInfo(
    val levelPercent: Int,
    val status: BatteryStatus,
    val health: BatteryHealth,
    val source: ChargeSource,
    val technology: String,
    val temperatureCelsius: Float,
    val voltageMv: Int,
    val currentMa: Int?,
    val capacityMah: Int?,
    val chargeCycles: Int?,
    val isPresent: Boolean,
    val estimatedWatts: Float? = null,
    val chargeTimeRemainingMs: Long? = null,
    val isPowerSaveMode: Boolean = false,
)

enum class BatteryStatus { Charging, Discharging, Full, NotCharging, Unknown }
enum class BatteryHealth { Good, Overheat, Dead, OverVoltage, Failure, Cold, Unknown }
enum class ChargeSource { AC, USB, Wireless, Dock, Unknown }
