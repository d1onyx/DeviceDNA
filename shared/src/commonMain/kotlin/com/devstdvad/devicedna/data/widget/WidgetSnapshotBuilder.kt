package com.devstdvad.devicedna.data.widget

import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.HealthScore
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import com.devstdvad.devicedna.domain.model.ThermalInfo
import com.devstdvad.devicedna.domain.model.ThermalZoneType

object WidgetSnapshotBuilder {

    fun buildSnapshot(
        isPremium: Boolean,
        battery: BatteryInfo?,
        ram: RamInfo?,
        storage: StorageInfo?,
        cpu: CpuInfo?,
        thermal: ThermalInfo?,
        device: DeviceInfo?,
        health: HealthScore?,
        thermalStatus: Int,
        designCapacityMah: Int?,
        nowMillis: Long,
    ): WidgetSnapshot {
        val thermalMax = thermal?.zones?.mapNotNull { it.temperatureCelsius }?.maxOrNull() ?: 0f
        val cpuZoneTemp = thermal?.zones
            ?.firstOrNull { it.type == ThermalZoneType.Cpu }
            ?.temperatureCelsius
        val topInsight = health?.insights?.maxByOrNull { it.severity.ordinal }
        val onlineCores = cpu?.cores?.filter { it.isOnline }.orEmpty()
        val curFreqMhz = onlineCores.mapNotNull { it.currentFrequencyKhz }
            .takeIf { it.isNotEmpty() }?.average()?.div(1000)?.toInt() ?: 0
        val maxFreqMhz = onlineCores.maxOfOrNull { it.maxFrequencyKhz }?.div(1000)?.toInt() ?: 0

        return WidgetSnapshot(
            isPremium = isPremium,
            hasData = battery != null || ram != null || storage != null || cpu != null,
            lastUpdatedMillis = nowMillis,
            batteryLevel = battery?.levelPercent ?: -1,
            batteryTempC = battery?.temperatureCelsius ?: 0f,
            batteryStatus = battery?.status?.name.orEmpty(),
            batteryHealth = battery?.health?.name.orEmpty(),
            ramUsedPercent = ram?.usedPercent ?: 0f,
            ramUsedBytes = ram?.usedBytes ?: 0L,
            ramTotalBytes = ram?.totalBytes ?: 0L,
            storageUsedPercent = storage?.usedPercent ?: 0f,
            storageUsedBytes = storage?.usedBytes ?: 0L,
            storageTotalBytes = storage?.totalBytes ?: 0L,
            cpuUsagePercent = cpu?.usagePercent ?: -1f,
            cpuTempC = cpu?.temperatureCelsius ?: cpuZoneTemp ?: 0f,
            thermalMaxC = thermalMax,
            healthOverall = health?.overall ?: -1,
            healthInsight = topInsight?.title.orEmpty(),
            healthSeverity = topInsight?.severity?.name.orEmpty(),
            batteryWearPercent = batteryWear(battery, designCapacityMah),
            batteryCycles = battery?.chargeCycles ?: -1,
            batteryWatts = battery?.estimatedWatts ?: 0f,
            batteryChargeTimeMs = battery?.chargeTimeRemainingMs ?: 0L,
            batteryCharging = battery?.status == BatteryStatus.Charging || battery?.status == BatteryStatus.Full,
            thermalStatus = thermalStatus,
            cpuCurFreqMhz = curFreqMhz,
            cpuMaxFreqMhz = maxFreqMhz,
            isRooted = device?.isRooted ?: false,
            integrityIssues = integrityIssues(device, health),
            fraudLevel = health?.fraudRisk?.level?.name.orEmpty(),
        )
    }

    fun batteryWear(battery: BatteryInfo?, designCapacityMah: Int?): Int {
        val charge = battery?.capacityMah ?: return -1
        val level = battery.levelPercent
        val design = designCapacityMah ?: return -1
        if (level <= 0 || design <= 0) return -1
        val fullCharge = charge / (level / 100f)
        return ((fullCharge / design) * 100f).toInt().coerceIn(0, 100)
    }

    fun integrityIssues(device: DeviceInfo?, health: HealthScore?): String {
        if (device == null) return ""
        val issues = buildList {
            if (device.isRooted) add("root")
            if (device.suspiciousRootPaths.isNotEmpty()) add("suspicious_files")
            if (device.isAdbEnabled) add("adb")
            if (device.isDeveloperOptionsEnabled) add("dev_options")
            if (device.isEmulator) add("emulator")
            val fraud = health?.fraudRisk?.level?.name
            if (fraud != null && fraud != "Low") add("fraud:$fraud")
        }
        return issues.joinToString(", ")
    }
}
