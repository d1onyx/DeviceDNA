package com.devstdvad.devicedna.data.batteryintelligence

import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.ChargeSource
import kotlinx.serialization.Serializable

@Serializable
data class BatteryHistoryPayload(
    val snapshots: List<BatteryHistorySnapshot> = emptyList(),
)

@Serializable
data class BatteryHistorySnapshot(
    val timestampMillis: Long,
    val levelPercent: Int,
    val status: String,
    val source: String,
    val temperatureCelsius: Float,
    val currentMa: Int?,
    val estimatedWatts: Float?,
    val chargeCycles: Int?,
) {
    val isCharging: Boolean
        get() = status == BatteryStatus.Charging.name || status == BatteryStatus.Full.name

    val isPlugged: Boolean
        get() = source != ChargeSource.Unknown.name
}
