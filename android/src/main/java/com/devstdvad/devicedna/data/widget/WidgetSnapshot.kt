package com.devstdvad.devicedna.data.widget

/**
 * Snapshot of the metrics shown by home-screen widgets, cached in DataStore so widget
 * rendering is cheap (no heavy reads on the UI thread). Filled by [WidgetMetricsLoader].
 */
data class WidgetSnapshot(
    val isPremium: Boolean = false,
    val hasData: Boolean = false,
    val lastUpdatedMillis: Long = 0L,

    // Battery
    val batteryLevel: Int = -1,
    val batteryTempC: Float = 0f,
    val batteryStatus: String = "",
    val batteryHealth: String = "",

    // Memory (RAM + Storage)
    val ramUsedPercent: Float = 0f,
    val ramUsedBytes: Long = 0L,
    val ramTotalBytes: Long = 0L,
    val storageUsedPercent: Float = 0f,
    val storageUsedBytes: Long = 0L,
    val storageTotalBytes: Long = 0L,

    // CPU + Thermal
    val cpuUsagePercent: Float = -1f,
    val cpuTempC: Float = 0f,
    val thermalMaxC: Float = 0f,

    // Device Health (HealthAnalyzer)
    val healthOverall: Int = -1,
    val healthInsight: String = "",
    val healthSeverity: String = "",

    // Battery Doctor
    val batteryWearPercent: Int = -1,
    val batteryCycles: Int = -1,
    val batteryWatts: Float = 0f,
    val batteryChargeTimeMs: Long = 0L,
    val batteryCharging: Boolean = false,

    // Thermal Guard
    val thermalStatus: Int = -1,
    val cpuCurFreqMhz: Int = 0,
    val cpuMaxFreqMhz: Int = 0,

    // Guardian (security / integrity)
    val isRooted: Boolean = false,
    val integrityIssues: String = "",
    val fraudLevel: String = "",
)
