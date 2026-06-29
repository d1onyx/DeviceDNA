package com.devstdvad.devicedna.di

import com.devstdvad.devicedna.BuildConfig
import com.devstdvad.devicedna.core.feedback.HapticManager
import com.devstdvad.devicedna.core.feedback.SoundManager
import com.devstdvad.devicedna.core.notification.SmartAlertNotifier
import com.devstdvad.devicedna.data.alerts.SmartAlertsManager
import com.devstdvad.devicedna.data.alerts.SmartAlertsStateStore
import com.devstdvad.devicedna.data.auth.AuthRepository
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.sync.DeviceSnapshotBuilder
import com.devstdvad.devicedna.data.sync.DeviceSyncManager
import com.devstdvad.devicedna.data.sync.SyncApi
import com.devstdvad.devicedna.data.sync.SyncStateStore
import com.devstdvad.devicedna.data.sync.createSyncHttpClient
import com.devstdvad.devicedna.data.widget.WidgetMetricsLoader
import com.devstdvad.devicedna.data.widget.WidgetSnapshotCache
import com.devstdvad.devicedna.data.widget.WidgetSystemProbe
import com.devstdvad.devicedna.presentation.auth.AuthViewModel
import com.devstdvad.devicedna.presentation.sync.SyncViewModel
import com.devstdvad.devicedna.data.export.ExportManager
import com.devstdvad.devicedna.data.repository.AppsRepositoryImpl
import com.devstdvad.devicedna.data.repository.BatteryRepositoryImpl
import com.devstdvad.devicedna.data.repository.CameraRepositoryImpl
import com.devstdvad.devicedna.data.repository.ConnectivityRepositoryImpl
import com.devstdvad.devicedna.data.repository.CpuRepositoryImpl
import com.devstdvad.devicedna.data.repository.DeviceRepositoryImpl
import com.devstdvad.devicedna.data.repository.DisplayRepositoryImpl
import com.devstdvad.devicedna.data.repository.NetworkRepositoryImpl
import com.devstdvad.devicedna.data.repository.RamRepositoryImpl
import com.devstdvad.devicedna.data.repository.SensorRepositoryImpl
import com.devstdvad.devicedna.data.repository.StorageRepositoryImpl
import com.devstdvad.devicedna.data.repository.SystemRepositoryImpl
import com.devstdvad.devicedna.data.repository.ThermalRepositoryImpl
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.core.CurrentActivityHolder
import com.devstdvad.devicedna.data.subscription.DevSubscriptionBillingGateway
import com.devstdvad.devicedna.data.subscription.PlayBillingGateway
import com.devstdvad.devicedna.data.subscription.SubscriptionBillingGateway
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.data.subscription.SubscriptionStore
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
import com.devstdvad.devicedna.domain.health.HealthAnalyzer
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
import com.devstdvad.devicedna.presentation.apps.AppsViewModel
import com.devstdvad.devicedna.presentation.batteryintelligence.BatteryAnalyticsExportManager
import com.devstdvad.devicedna.presentation.batteryintelligence.BatteryAnalyticsExportViewModel
import com.devstdvad.devicedna.presentation.batteryintelligence.BatteryIntelligenceViewModel
import com.devstdvad.devicedna.presentation.battery.BatteryViewModel
import com.devstdvad.devicedna.presentation.camera.CameraViewModel
import com.devstdvad.devicedna.presentation.cpu.CpuViewModel
import com.devstdvad.devicedna.presentation.device.DeviceViewModel
import com.devstdvad.devicedna.presentation.display.DisplayViewModel
import com.devstdvad.devicedna.presentation.network.NetworkViewModel
import com.devstdvad.devicedna.presentation.overview.OverviewViewModel
import com.devstdvad.devicedna.presentation.sensors.SensorsViewModel
import com.devstdvad.devicedna.presentation.settings.ExportViewModel
import com.devstdvad.devicedna.presentation.settings.SettingsViewModel
import com.devstdvad.devicedna.presentation.subscription.SubscriptionViewModel
import com.devstdvad.devicedna.presentation.system.SystemViewModel
import com.devstdvad.devicedna.presentation.tests.TestsViewModel
import com.devstdvad.devicedna.presentation.thermal.ThermalViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {

    // Auth
    single { AuthRepository(androidContext()) }
    viewModelOf(::AuthViewModel)

    // Feedback
    single { HapticManager(androidContext()) }
    single { SoundManager() }

    // Export
    single { ExportManager(androidContext(), get(), get(), get(), get(), get(), get(), get()) }
    single { BatteryAnalyticsExportManager(androidContext()) }

    // Data sources
    single { AndroidDeviceDataSource(androidContext()) }
    single { AndroidSystemDataSource(androidContext()) }
    single { AndroidCpuDataSource(androidContext()) }
    single { AndroidBatteryDataSource(androidContext()) }
    single { AndroidRamStorageDataSource(androidContext()) }
    single { AndroidNetworkDataSource(androidContext()) }
    single { AndroidDisplayDataSource(androidContext()) }
    single { AndroidCameraDataSource(androidContext()) }
    single { AndroidThermalDataSource() }
    single { AndroidSensorDataSource(androidContext()) }
    single { AndroidAppsDataSource(androidContext()) }
    single { SettingsStore(androidContext()) }
    single { SubscriptionStore(androidContext()) }
    single { BatteryIntelligenceHistoryStore(androidContext()) }

    // Repositories
    single<DeviceRepository> { DeviceRepositoryImpl(get()) }
    single<SystemRepository> { SystemRepositoryImpl(get()) }
    single<CpuRepository> { CpuRepositoryImpl(get()) }
    single<BatteryRepository> { BatteryRepositoryImpl(get()) }
    single<RamRepository> { RamRepositoryImpl(get()) }
    single<StorageRepository> { StorageRepositoryImpl(get()) }
    single<NetworkRepository> { NetworkRepositoryImpl(get()) }
    single<ConnectivityRepository> { ConnectivityRepositoryImpl(get()) }
    single<DisplayRepository> { DisplayRepositoryImpl(get()) }
    single<CameraRepository> { CameraRepositoryImpl(get()) }
    single<ThermalRepository> { ThermalRepositoryImpl(get()) }
    single<SensorRepository> { SensorRepositoryImpl(get()) }
    single<AppsRepository> { AppsRepositoryImpl(get()) }
    single<HealthRepository> { HealthAnalyzer() }

    // Use cases
    single { GetDeviceInfoUseCase(get()) }
    single { GetSystemInfoUseCase(get()) }
    single { GetCpuInfoUseCase(get()) }
    single { ObserveBatteryUseCase(get()) }
    single { ObserveRamUseCase(get()) }
    single { GetStorageInfoUseCase(get()) }
    single { GetNetworkInfoUseCase(get()) }
    single { GetConnectivityInfoUseCase(get()) }
    single { GetDisplayInfoUseCase(get()) }
    single { GetCameraInfoUseCase(get()) }
    single { GetThermalInfoUseCase(get()) }
    single { GetSensorsUseCase(get()) }
    single { GetAppsUseCase(get()) }
    single { GetHealthScoreUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }

    // Sync (Cloudflare Worker)
    single { createSyncHttpClient() }
    single { SyncApi(get(), BuildConfig.SYNC_BASE_URL) }
    single { SyncStateStore(androidContext()) }
    single {
        DeviceSnapshotBuilder(
            get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(),
        )
    }
    single { DeviceSyncManager(get(), get(), get(), get()) }

    // Subscription. Real Google Play Billing when USE_REAL_BILLING (release), dev gateway otherwise.
    single { CurrentActivityHolder() }
    single<SubscriptionBillingGateway> {
        if (BuildConfig.USE_REAL_BILLING) {
            PlayBillingGateway(androidContext(), get())
        } else {
            DevSubscriptionBillingGateway()
        }
    }
    single { SubscriptionRepository(get<SubscriptionStore>(), get()) }

    // Widgets
    single { WidgetSnapshotCache(androidContext()) }
    single { WidgetSystemProbe(androidContext()) }
    single {
        WidgetMetricsLoader(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(),
        )
    }

    // Smart Alerts
    single { SmartAlertsStateStore(androidContext()) }
    single { SmartAlertNotifier(androidContext()) }
    single { SmartAlertsManager(get(), get(), get(), get()) }

    // ViewModels
    viewModelOf(::OverviewViewModel)
    viewModelOf(::DeviceViewModel)
    viewModelOf(::SystemViewModel)
    viewModelOf(::CpuViewModel)
    viewModelOf(::BatteryViewModel)
    viewModelOf(::BatteryAnalyticsExportViewModel)
    viewModelOf(::BatteryIntelligenceViewModel)
    viewModelOf(::NetworkViewModel)
    viewModelOf(::DisplayViewModel)
    viewModelOf(::CameraViewModel)
    viewModelOf(::ThermalViewModel)
    viewModelOf(::SensorsViewModel)
    viewModelOf(::AppsViewModel)
    viewModelOf(::TestsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ExportViewModel)
    viewModelOf(::SyncViewModel)
    viewModelOf(::SubscriptionViewModel)
}
