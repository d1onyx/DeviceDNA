package com.devstdvad.devicedna.domain.usecase

import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.core.common.getOrNull
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
import com.devstdvad.devicedna.domain.repository.AppsRepository
import com.devstdvad.devicedna.domain.repository.BatteryRepository
import com.devstdvad.devicedna.domain.repository.CameraRepository
import com.devstdvad.devicedna.domain.repository.ConnectivityRepository
import com.devstdvad.devicedna.domain.repository.CpuRepository
import com.devstdvad.devicedna.domain.repository.DeviceRepository
import com.devstdvad.devicedna.domain.repository.DisplayRepository
import com.devstdvad.devicedna.domain.repository.HealthRepository
import com.devstdvad.devicedna.domain.repository.NetworkRepository
import com.devstdvad.devicedna.domain.repository.RamRepository
import com.devstdvad.devicedna.domain.repository.SensorRepository
import com.devstdvad.devicedna.domain.repository.StorageRepository
import com.devstdvad.devicedna.domain.repository.SystemRepository
import com.devstdvad.devicedna.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow

class GetDeviceInfoUseCase(private val repo: DeviceRepository) {
    suspend operator fun invoke(): AppResult<DeviceInfo> = repo.getDeviceInfo()
}

class GetSystemInfoUseCase(private val repo: SystemRepository) {
    suspend operator fun invoke(): AppResult<SystemInfo> = repo.getSystemInfo()
}

class GetCpuInfoUseCase(private val repo: CpuRepository) {
    suspend operator fun invoke(): AppResult<CpuInfo> = repo.getCpuInfo()
    fun observe(): Flow<AppResult<CpuInfo>> = repo.observeCpuCores()
}

class ObserveBatteryUseCase(private val repo: BatteryRepository) {
    operator fun invoke(): Flow<AppResult<BatteryInfo>> = repo.observeBatteryInfo()
}

class ObserveRamUseCase(private val repo: RamRepository) {
    operator fun invoke(): Flow<AppResult<RamInfo>> = repo.observeRamInfo()
}

class GetStorageInfoUseCase(private val repo: StorageRepository) {
    suspend operator fun invoke(): AppResult<StorageInfo> = repo.getStorageInfo()
}

class GetNetworkInfoUseCase(private val repo: NetworkRepository) {
    suspend operator fun invoke(): AppResult<NetworkInfo> = repo.getNetworkInfo()
    fun observe(): Flow<AppResult<NetworkInfo>> = repo.observeNetworkInfo()
}

class GetConnectivityInfoUseCase(private val repo: ConnectivityRepository) {
    suspend operator fun invoke(): AppResult<ConnectivityInfo> = repo.getConnectivityInfo()
}

class GetDisplayInfoUseCase(private val repo: DisplayRepository) {
    suspend operator fun invoke(): AppResult<DisplayInfo> = repo.getDisplayInfo()
    fun observe(): Flow<AppResult<DisplayInfo>> = repo.observeDisplayInfo()
}

class GetCameraInfoUseCase(private val repo: CameraRepository) {
    suspend operator fun invoke(): AppResult<CameraInfo> = repo.getCameraInfo()
}

class GetThermalInfoUseCase(private val repo: ThermalRepository) {
    suspend operator fun invoke(): AppResult<ThermalInfo> = repo.getThermalInfo()
    fun observe(): Flow<AppResult<ThermalInfo>> = repo.observeThermalInfo()
}

class GetSensorsUseCase(private val repo: SensorRepository) {
    suspend operator fun invoke(): AppResult<SensorInfo> = repo.getSensorInfo()
}

class GetAppsUseCase(private val repo: AppsRepository) {
    suspend operator fun invoke(): AppResult<AppListInfo> = repo.getAppList()
}

class GetHealthScoreUseCase(
    private val batteryRepo: BatteryRepository,
    private val ramRepo: RamRepository,
    private val storageRepo: StorageRepository,
    private val thermalRepo: ThermalRepository,
    private val deviceRepo: DeviceRepository,
    private val systemRepo: SystemRepository,
    private val networkRepo: NetworkRepository,
    private val healthRepo: HealthRepository,
) {
    suspend operator fun invoke(): HealthScore = healthRepo.getHealthScore(
        battery = batteryRepo.getBatterySnapshot().getOrNull(),
        ram = ramRepo.getRamSnapshot().getOrNull(),
        storage = storageRepo.getStorageInfo().getOrNull(),
        thermal = thermalRepo.getThermalInfo().getOrNull(),
        device = deviceRepo.getDeviceInfo().getOrNull(),
        system = systemRepo.getSystemInfo().getOrNull(),
        network = networkRepo.getNetworkInfo().getOrNull(),
    )
}
