package com.devstdvad.devicedna.data.widget

/**
 * Snapshot of the metrics shown by home-screen widgets. Platform implementations decide
 * how this is cached and rendered.
 */
data class WidgetSnapshot(
    val isPremium: Boolean = false,
    val hasData: Boolean = false,
    val lastUpdatedMillis: Long = 0L,

    val batteryLevel: Int = -1,
    val batteryTempC: Float = 0f,
    val batteryStatus: String = "",
    val batteryHealth: String = "",

    val ramUsedPercent: Float = 0f,
    val ramUsedBytes: Long = 0L,
    val ramTotalBytes: Long = 0L,
    val storageUsedPercent: Float = 0f,
    val storageUsedBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,

    val cpuUsagePercent: Float = -1f,
    val cpuTempC: Float = 0f,
    val thermalMaxC: Float = 0f,

    val healthOverall: Int = -1,
    val healthInsight: String = "",
    val healthSeverity: String = "",

    val batteryWearPercent: Int = -1,
    val batteryCycles: Int = -1,
    val batteryWatts: Float = 0f,
    val batteryChargeTimeMs: Long = 0L,
    val batteryCharging: Boolean = false,

    val thermalStatus: Int = -1,
    val cpuCurFreqMhz: Int = 0,
    val cpuMaxFreqMhz: Int = 0,

    val isRooted: Boolean = false,
    val integrityIssues: String = "",
    val fraudLevel: String = "",
)
