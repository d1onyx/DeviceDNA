package com.devstdvad.devicedna.data.repository

import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.data.source.AndroidAppsDataSource
import com.devstdvad.devicedna.data.source.AndroidBatteryDataSource
import com.devstdvad.devicedna.data.source.AndroidCameraDataSource
import com.devstdvad.devicedna.data.source.AndroidCpuDataSource
import com.devstdvad.devicedna.data.source.AndroidDeviceDataSource
import com.devstdvad.devicedna.data.source.AndroidDisplayDataSource
import com.devstdvad.devicedna.data.source.AndroidNetworkDataSource
import com.devstdvad.devicedna.data.source.AndroidRamStorageDataSource
import com.devstdvad.devicedna.data.source.AndroidSensorDataSource
import com.devstdvad.devicedna.data.source.AndroidSystemDataSource
import com.devstdvad.devicedna.data.source.AndroidThermalDataSource
import com.devstdvad.devicedna.domain.model.AppListInfo
import com.devstdvad.devicedna.domain.model.BatteryInfo
import com.devstdvad.devicedna.domain.model.CameraInfo
import com.devstdvad.devicedna.domain.model.ConnectivityInfo
import com.devstdvad.devicedna.domain.model.CpuInfo
import com.devstdvad.devicedna.domain.model.DeviceInfo
import com.devstdvad.devicedna.domain.model.DisplayInfo
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
import com.devstdvad.devicedna.domain.repository.NetworkRepository
import com.devstdvad.devicedna.domain.repository.RamRepository
import com.devstdvad.devicedna.domain.repository.SensorRepository
import com.devstdvad.devicedna.domain.repository.StorageRepository
import com.devstdvad.devicedna.domain.repository.SystemRepository
import com.devstdvad.devicedna.domain.repository.ThermalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class DeviceRepositoryImpl(private val source: AndroidDeviceDataSource) : DeviceRepository {
    override suspend fun getDeviceInfo(): AppResult<DeviceInfo> = source.getDeviceInfo()
}

class SystemRepositoryImpl(private val source: AndroidSystemDataSource) : SystemRepository {
    override suspend fun getSystemInfo(): AppResult<SystemInfo> = withContext(Dispatchers.IO) { source.getSystemInfo() }
}

class CpuRepositoryImpl(private val source: AndroidCpuDataSource) : CpuRepository {
    override suspend fun getCpuInfo(): AppResult<CpuInfo> = source.getCpuInfo()
    override fun observeCpuCores(): Flow<AppResult<CpuInfo>> = flow {
        while (true) { emit(getCpuInfo()); delay(2_000) }
    }.flowOn(Dispatchers.IO)
}

class BatteryRepositoryImpl(private val source: AndroidBatteryDataSource) : BatteryRepository {
    override fun observeBatteryInfo(): Flow<AppResult<BatteryInfo>> = source.observeBattery()
    override suspend fun getBatterySnapshot(): AppResult<BatteryInfo> = withContext(Dispatchers.IO) { source.getBatterySnapshot() }
}

class RamRepositoryImpl(private val source: AndroidRamStorageDataSource) : RamRepository {
    override fun observeRamInfo(): Flow<AppResult<RamInfo>> = source.observeRam()
    override suspend fun getRamSnapshot(): AppResult<RamInfo> = withContext(Dispatchers.IO) { source.getRamSnapshot() }
}

class StorageRepositoryImpl(private val source: AndroidRamStorageDataSource) : StorageRepository {
    override suspend fun getStorageInfo(): AppResult<StorageInfo> = withContext(Dispatchers.IO) { source.getStorageInfo() }
}

class NetworkRepositoryImpl(private val source: AndroidNetworkDataSource) : NetworkRepository {
    override suspend fun getNetworkInfo(): AppResult<NetworkInfo> = withContext(Dispatchers.IO) { source.getNetworkInfo() }
    override fun observeNetworkInfo(): Flow<AppResult<NetworkInfo>> = source.observeNetwork()
}

class ConnectivityRepositoryImpl(private val source: AndroidNetworkDataSource) : ConnectivityRepository {
    override suspend fun getConnectivityInfo(): AppResult<ConnectivityInfo> = withContext(Dispatchers.IO) { source.getConnectivityInfo() }
}

class DisplayRepositoryImpl(private val source: AndroidDisplayDataSource) : DisplayRepository {
    override suspend fun getDisplayInfo(): AppResult<DisplayInfo> = withContext(Dispatchers.IO) { source.getDisplayInfo() }
}

class CameraRepositoryImpl(private val source: AndroidCameraDataSource) : CameraRepository {
    override suspend fun getCameraInfo(): AppResult<CameraInfo> = source.getCameraInfo()
}

class ThermalRepositoryImpl(private val source: AndroidThermalDataSource) : ThermalRepository {
    override suspend fun getThermalInfo(): AppResult<ThermalInfo> = source.getThermalInfo()
    override fun observeThermalInfo(): Flow<AppResult<ThermalInfo>> = source.observeThermal()
}

class SensorRepositoryImpl(private val source: AndroidSensorDataSource) : SensorRepository {
    override suspend fun getSensorInfo(): AppResult<SensorInfo> = source.getSensorInfo()
}

class AppsRepositoryImpl(private val source: AndroidAppsDataSource) : AppsRepository {
    override suspend fun getAppList(): AppResult<AppListInfo> = source.getAppList()
}
