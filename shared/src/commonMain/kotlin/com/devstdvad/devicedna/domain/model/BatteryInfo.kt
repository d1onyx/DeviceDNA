package com.devstdvad.devicedna.domain.model

import kotlinx.serialization.Serializable

@Serializable
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

@Serializable
enum class BatteryStatus { Charging, Discharging, Full, NotCharging, Unknown }

@Serializable
enum class BatteryHealth { Good, Overheat, Dead, OverVoltage, Failure, Cold, Unknown }

@Serializable
enum class ChargeSource { AC, USB, Wireless, Dock, Unknown }
