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
    /**
     * Marks the moment recording stopped (premium/tracking turned off). The timeline must not bridge
     * or paint across such a marker, so periods without recording stay empty even if the device kept
     * charging. Defaults to false for backward compatibility with previously stored snapshots.
     */
    val recordingPaused: Boolean = false,
) {
    val isCharging: Boolean
        get() = status == BatteryStatus.Charging.name || status == BatteryStatus.Full.name

    val isPlugged: Boolean
        get() = source != ChargeSource.Unknown.name
}
