package com.devstdvad.devicedna.data.widget

import com.devstdvad.devicedna.core.common.getOrNull
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.HealthScore
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import com.devstdvad.devicedna.domain.model.ThermalInfo
import com.devstdvad.devicedna.domain.model.ThermalZoneType
import com.devstdvad.devicedna.domain.repository.BatteryRepository
import com.devstdvad.devicedna.domain.repository.CpuRepository
import com.devstdvad.devicedna.domain.repository.RamRepository
import com.devstdvad.devicedna.domain.repository.StorageRepository
import com.devstdvad.devicedna.domain.repository.ThermalRepository
import com.devstdvad.devicedna.domain.usecase.GetDeviceInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetHealthScoreUseCase
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

/**
 * Loads the metrics for all home-screen widgets and writes them into [WidgetSnapshotCache].
 * Metrics are fetched only when there is an active subscription.
 */
class WidgetMetricsLoader(
    private val subscriptionRepository: SubscriptionRepository,
    private val batteryRepository: BatteryRepository,
    private val ramRepository: RamRepository,
    private val storageRepository: StorageRepository,
    private val cpuRepository: CpuRepository,
    private val thermalRepository: ThermalRepository,
    private val getDeviceInfo: GetDeviceInfoUseCase,
    private val getHealthScore: GetHealthScoreUseCase,
    private val probe: WidgetSystemProbe,
    private val cache: WidgetSnapshotCache,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun refresh(): WidgetSnapshot {
        // Single premium tier: any active subscription unlocks widgets.
        val isPremium = subscriptionRepository.entitlements.first().isActive
        if (!isPremium) {
            val locked = WidgetSnapshot(isPremium = false, hasData = false, lastUpdatedMillis = now())
            cache.save(locked)
            return locked
        }

        val snapshot = coroutineScope {
            val battery = async { batteryRepository.getBatterySnapshot().getOrNull() }
            val ram = async { ramRepository.getRamSnapshot().getOrNull() }
            val storage = async { storageRepository.getStorageInfo().getOrNull() }
            val cpu = async { cpuRepository.getCpuInfo().getOrNull() }
            val thermal = async { thermalRepository.getThermalInfo().getOrNull() }
            val device = async { getDeviceInfo().getOrNull() }
            val health = async { runCatching { getHealthScore() }.getOrNull() }
            buildSnapshot(
                isPremium = true,
                battery = battery.await(),
                ram = ram.await(),
                storage = storage.await(),
                cpu = cpu.await(),
                thermal = thermal.await(),
                device = device.await(),
                health = health.await(),
                thermalStatus = probe.thermalStatus(),
                designCapacityMah = probe.designCapacityMah(),
                nowMillis = now(),
            )
        }
        cache.save(snapshot)
        return snapshot
    }

    companion object {
        /** Pure mapping — testable without Android. */
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

            // Most-severe health insight (Critical > Warning > Info > Good).
            val topInsight = health?.insights?.maxByOrNull { it.severity.ordinal }

            // CPU current/max clock (MHz) averaged over online cores.
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
                // Device Health
                healthOverall = health?.overall ?: -1,
                healthInsight = topInsight?.title.orEmpty(),
                healthSeverity = topInsight?.severity?.name.orEmpty(),
                // Battery Doctor
                batteryWearPercent = batteryWear(battery, designCapacityMah),
                batteryCycles = battery?.chargeCycles ?: -1,
                batteryWatts = battery?.estimatedWatts ?: 0f,
                batteryChargeTimeMs = battery?.chargeTimeRemainingMs ?: 0L,
                batteryCharging = battery?.status == BatteryStatus.Charging || battery?.status == BatteryStatus.Full,
                // Thermal Guard
                thermalStatus = thermalStatus,
                cpuCurFreqMhz = curFreqMhz,
                cpuMaxFreqMhz = maxFreqMhz,
                // Guardian
                isRooted = device?.isRooted ?: false,
                integrityIssues = integrityIssues(device, health),
                fraudLevel = health?.fraudRisk?.level?.name.orEmpty(),
            )
        }

        /** Estimated battery health (100 = no wear), or -1 when it cannot be computed. */
        fun batteryWear(battery: BatteryInfo?, designCapacityMah: Int?): Int {
            val charge = battery?.capacityMah ?: return -1
            val level = battery.levelPercent
            val design = designCapacityMah ?: return -1
            if (level <= 0 || design <= 0) return -1
            val fullCharge = charge / (level / 100f)
            return ((fullCharge / design) * 100f).toInt().coerceIn(0, 100)
        }

        /**
         * Comma-joined integrity-issue KEYS (not display text); empty string means the device
         * looks clean. Keys are localized at render time by the widget so they follow the in-app
         * language. The fraud key carries the level, e.g. "fraud:High". Keep these keys in sync
         * with `Context.integrityIssueText` in the widget layer.
         */
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
}
