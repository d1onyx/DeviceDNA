package com.devstdvad.devicedna.data.widget

import com.devstdvad.devicedna.core.common.getOrNull
import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.devstdvad.devicedna.data.subscription.PremiumEntitlementsStore
import com.devstdvad.devicedna.domain.usecase.GetDeviceInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetHealthScoreUseCase
import com.devstdvad.devicedna.domain.usecase.GetStorageInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetThermalInfoUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveBatteryUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveRamUseCase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSProcessInfoThermalState
import platform.Foundation.NSUserDefaults
import platform.Foundation.thermalState

/**
 * Flat, Codable-friendly payload the Swift WidgetKit extension decodes from the App Group.
 * Field names are part of the app↔extension contract — keep in sync with DeviceDNAWidget.swift.
 */
@Serializable
data class WidgetPayload(
    val isPremium: Boolean = false,
    val hasData: Boolean = false,
    val lastUpdatedMillis: Long = 0,
    val batteryLevel: Int = -1,
    val batteryStatus: String = "",
    val batteryCharging: Boolean = false,
    val ramUsedPercent: Float = 0f,
    val storageUsedPercent: Float = 0f,
    val healthOverall: Int = -1,
    val healthInsight: String = "",
    val healthSeverity: String = "",
    val thermalStatus: Int = -1,
    val isRooted: Boolean = false,
    val fraudLevel: String = "",
)

/**
 * Builds the shared [WidgetSnapshot] (same Kotlin [WidgetSnapshotBuilder] the Android Glance
 * widgets use) and publishes a JSON payload to the App Group so the WidgetKit extension can
 * render it. Reloading WidgetKit timelines is Swift-only API, so the host injects
 * [reloadWidgetTimelines] (WidgetCenter.shared.reloadAllTimelines()).
 */
class IosWidgetBridge(
    private val observeBattery: ObserveBatteryUseCase,
    private val observeRam: ObserveRamUseCase,
    private val getStorage: GetStorageInfoUseCase,
    private val getThermal: GetThermalInfoUseCase,
    private val getDevice: GetDeviceInfoUseCase,
    private val getHealth: GetHealthScoreUseCase,
    private val entitlementsStore: PremiumEntitlementsStore,
    private val reloadWidgetTimelines: () -> Unit,
    appGroupId: String,
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = appGroupId) ?: NSUserDefaults.standardUserDefaults,
) {
    private val json = Json { encodeDefaults = true }

    suspend fun refresh(): WidgetSnapshot {
        val battery = observeBattery().first().getOrNull()
        val ram = observeRam().first().getOrNull()
        val storage = getStorage().getOrNull()
        val thermal = getThermal().getOrNull()
        val device = getDevice().getOrNull()
        val health = getHealth()
        val isPremium = entitlementsStore.entitlements.first().isActive

        val snapshot = WidgetSnapshotBuilder.buildSnapshot(
            isPremium = isPremium,
            battery = battery,
            ram = ram,
            storage = storage,
            cpu = null,
            thermal = thermal,
            device = device,
            health = health,
            thermalStatus = thermalStatusValue(),
            designCapacityMah = null,
            nowMillis = currentTimeMillis(),
        )
        publish(snapshot)
        return snapshot
    }

    private fun publish(s: WidgetSnapshot) {
        val payload = WidgetPayload(
            isPremium = s.isPremium,
            hasData = s.hasData,
            lastUpdatedMillis = s.lastUpdatedMillis,
            batteryLevel = s.batteryLevel,
            batteryStatus = s.batteryStatus,
            batteryCharging = s.batteryCharging,
            ramUsedPercent = s.ramUsedPercent,
            storageUsedPercent = s.storageUsedPercent,
            healthOverall = s.healthOverall,
            healthInsight = s.healthInsight,
            healthSeverity = s.healthSeverity,
            thermalStatus = s.thermalStatus,
            isRooted = s.isRooted,
            fraudLevel = s.fraudLevel,
        )
        defaults.setObject(json.encodeToString(payload), PAYLOAD_KEY)
        reloadWidgetTimelines()
    }

    private fun thermalStatusValue(): Int = when (NSProcessInfo.processInfo.thermalState) {
        NSProcessInfoThermalState.NSProcessInfoThermalStateNominal -> 0
        NSProcessInfoThermalState.NSProcessInfoThermalStateFair -> 1
        NSProcessInfoThermalState.NSProcessInfoThermalStateSerious -> 2
        NSProcessInfoThermalState.NSProcessInfoThermalStateCritical -> 3
        else -> -1
    }

    companion object {
        const val PAYLOAD_KEY = "widget_snapshot_v1"
    }
}
