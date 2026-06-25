package com.devstdvad.devicedna.domain.repository

import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.AppListInfo
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.CameraInfo
import com.devstdvad.devicedna.domain.model.ConnectivityInfo
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.DisplayInfo
import com.devstdvad.devicedna.domain.model.HealthScore
import com.devstdvad.devicedna.domain.model.NetworkInfo
import com.devstdvad.devicedna.domain.model.RamInfo
import com.devstdvad.devicedna.domain.model.SensorInfo
import com.devstdvad.devicedna.domain.model.StorageInfo
import com.devstdvad.devicedna.domain.model.SystemInfo
import com.devstdvad.devicedna.domain.model.ThermalInfo
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    suspend fun getDeviceInfo(): AppResult<DeviceInfo>
}

interface SystemRepository {
    suspend fun getSystemInfo(): AppResult<SystemInfo>
}

interface CpuRepository {
    suspend fun getCpuInfo(): AppResult<CpuInfo>
    fun observeCpuCores(): Flow<AppResult<CpuInfo>>
}

interface BatteryRepository {
    fun observeBatteryInfo(): Flow<AppResult<BatteryInfo>>
    suspend fun getBatterySnapshot(): AppResult<BatteryInfo>
}

interface RamRepository {
    fun observeRamInfo(): Flow<AppResult<RamInfo>>
    suspend fun getRamSnapshot(): AppResult<RamInfo>
}

interface StorageRepository {
    suspend fun getStorageInfo(): AppResult<StorageInfo>
}

interface NetworkRepository {
    suspend fun getNetworkInfo(): AppResult<NetworkInfo>
    fun observeNetworkInfo(): Flow<AppResult<NetworkInfo>>
}

interface ConnectivityRepository {
    suspend fun getConnectivityInfo(): AppResult<ConnectivityInfo>
}

interface DisplayRepository {
    suspend fun getDisplayInfo(): AppResult<DisplayInfo>
}

interface CameraRepository {
    suspend fun getCameraInfo(): AppResult<CameraInfo>
}

interface ThermalRepository {
    suspend fun getThermalInfo(): AppResult<ThermalInfo>
    fun observeThermalInfo(): Flow<AppResult<ThermalInfo>>
}

interface SensorRepository {
    suspend fun getSensorInfo(): AppResult<SensorInfo>
}

interface AppsRepository {
    suspend fun getAppList(): AppResult<AppListInfo>
}

interface HealthRepository {
    suspend fun getHealthScore(
        battery: BatteryInfo?,
        ram: RamInfo?,
        storage: StorageInfo?,
        thermal: ThermalInfo?,
        device: DeviceInfo?,
        system: SystemInfo?,
        network: NetworkInfo? = null,
    ): HealthScore
}
