package com.devstdvad.devicedna.domain.model

import kotlinx.serialization.Serializable

/**
 * Full device diagnostics snapshot for backend synchronization.
 * All fields are nullable — a section may be unavailable (no permission / unsupported).
 */
@Serializable
data class DeviceSnapshot(
    val device: DeviceInfo? = null,
    val cpu: CpuInfo? = null,
    val system: SystemInfo? = null,
    val battery: BatteryInfo? = null,
    val ram: RamInfo? = null,
    val storage: StorageInfo? = null,
    val network: NetworkInfo? = null,
    val connectivity: ConnectivityInfo? = null,
    val display: DisplayInfo? = null,
    val camera: CameraInfo? = null,
    val thermal: ThermalInfo? = null,
    val sensors: SensorInfo? = null,
    val apps: AppListInfo? = null,
    val health: HealthScore? = null,
)
