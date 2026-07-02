package com.devstdvad.devicedna.data.sync

import com.devstdvad.devicedna.core.common.getOrNull
import com.devstdvad.devicedna.domain.model.DeviceSnapshot
import com.devstdvad.devicedna.domain.usecase.GetAppsUseCase
import com.devstdvad.devicedna.domain.usecase.GetCameraInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetConnectivityInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetCpuInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetDeviceInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetDisplayInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetHealthScoreUseCase
import com.devstdvad.devicedna.domain.usecase.GetNetworkInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetSensorsUseCase
import com.devstdvad.devicedna.domain.usecase.GetStorageInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetSystemInfoUseCase
import com.devstdvad.devicedna.domain.usecase.GetThermalInfoUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveBatteryUseCase
import com.devstdvad.devicedna.domain.usecase.ObserveRamUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Builds the full diagnostics snapshot in parallel from all use cases.
 * Reuses the same aggregation pattern as ExportManager, but covering every section.
 */
class DeviceSnapshotBuilder(
    private val getDevice: GetDeviceInfoUseCase,
    private val getSystem: GetSystemInfoUseCase,
    private val getCpu: GetCpuInfoUseCase,
    private val observeBattery: ObserveBatteryUseCase,
    private val observeRam: ObserveRamUseCase,
    private val getStorage: GetStorageInfoUseCase,
    private val getNetwork: GetNetworkInfoUseCase,
    private val getConnectivity: GetConnectivityInfoUseCase,
    private val getDisplay: GetDisplayInfoUseCase,
    private val getCamera: GetCameraInfoUseCase,
    private val getThermal: GetThermalInfoUseCase,
    private val getSensors: GetSensorsUseCase,
    private val getApps: GetAppsUseCase,
    private val getHealth: GetHealthScoreUseCase,
) : DeviceSnapshotProvider {
    override suspend fun build(): DeviceSnapshot = withContext(Dispatchers.Default) {
        val device = async { getDevice().getOrNull() }
        val system = async { getSystem().getOrNull() }
        val cpu = async { getCpu().getOrNull() }
        val battery = async { observeBattery().first().getOrNull() }
        val ram = async { observeRam().first().getOrNull() }
        val storage = async { getStorage().getOrNull() }
        val network = async { getNetwork().getOrNull() }
        val connectivity = async { getConnectivity().getOrNull() }
        val display = async { getDisplay().getOrNull() }
        val camera = async { getCamera().getOrNull() }
        val thermal = async { getThermal().getOrNull() }
        val sensors = async { getSensors().getOrNull() }
        val apps = async { getApps().getOrNull() }
        val health = async { runCatching { getHealth() }.getOrNull() }

        DeviceSnapshot(
            device = device.await(),
            cpu = cpu.await(),
            system = system.await(),
            battery = battery.await(),
            ram = ram.await(),
            storage = storage.await(),
            network = network.await(),
            connectivity = connectivity.await(),
            display = display.await(),
            camera = camera.await(),
            thermal = thermal.await(),
            sensors = sensors.await(),
            apps = apps.await(),
            health = health.await(),
        )
    }
}
