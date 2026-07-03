package com.devstdvad.devicedna.data.widget

import com.devstdvad.devicedna.core.common.getOrNull
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.domain.repository.BatteryRepository
import com.devstdvad.devicedna.domain.repository.CpuRepository
import com.devstdvad.devicedna.domain.repository.RamRepository
import com.devstdvad.devicedna.domain.repository.StorageRepository
import com.devstdvad.devicedna.domain.repository.ThermalRepository
import com.devstdvad.devicedna.domain.usecase.GetDeviceInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetHealthScoreUseCase
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
        val nowMillis = now()
        val hasWidgets = subscriptionRepository.entitlements.first()
            .hasFeature(PremiumFeature.Widgets, nowMillis)
        if (!hasWidgets) {
            val locked = WidgetSnapshot(isPremium = false, hasData = false, lastUpdatedMillis = nowMillis)
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
            WidgetSnapshotBuilder.buildSnapshot(
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
                nowMillis = nowMillis,
            )
        }
        cache.save(snapshot)
        return snapshot
    }

}
