package com.devstdvad.devicedna.di

import com.devstdvad.devicedna.data.export.DiagnosticsExporter
import com.devstdvad.devicedna.data.subscription.BackendSubscriptionVerifier
import com.devstdvad.devicedna.data.subscription.SubscriptionVerifier
import com.devstdvad.devicedna.data.sync.DeviceSnapshotBuilder
import com.devstdvad.devicedna.data.sync.DeviceSnapshotProvider
import com.devstdvad.devicedna.data.sync.DeviceSyncManager
import com.devstdvad.devicedna.domain.health.HealthAnalyzer
import com.devstdvad.devicedna.domain.repository.HealthRepository
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
import com.devstdvad.devicedna.presentation.battery.BatteryViewModel
import com.devstdvad.devicedna.presentation.batteryintelligence.BatteryAnalyticsExportViewModel
import com.devstdvad.devicedna.presentation.batteryintelligence.BatteryAnalyticsExporter
import com.devstdvad.devicedna.presentation.batteryintelligence.BatteryIntelligenceViewModel
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
import com.devstdvad.devicedna.presentation.sync.SyncViewModel
import com.devstdvad.devicedna.presentation.system.SystemViewModel
import com.devstdvad.devicedna.presentation.tests.TestsViewModel
import com.devstdvad.devicedna.presentation.thermal.ThermalViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Platform-agnostic Koin registrations shared by both hosts. The platform module must provide:
 * the 13 repository interfaces, SettingsStore, PremiumEntitlementsStore, SubscriptionRepository,
 * SubscriptionBillingGateway, BatteryIntelligenceHistoryStore, SyncApi, SyncStateStore,
 * AuthGateway, FileSharer, FileImporter, HapticManager, SoundManager.
 *
 * Android currently keeps its own AppModule (registered before this pattern existed); iOS uses
 * commonModule + iosModule. Compiled on both targets, so the Android build verifies it.
 */
fun commonModule(useRealBilling: Boolean): Module = module {

    // Health scoring (pure shared logic)
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

    // Export (renders shared, shares via platform FileSharer)
    single { DiagnosticsExporter(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { BatteryAnalyticsExporter() }

    // Sync pipeline (SyncApi + SyncStateStore come from the platform module)
    single<DeviceSnapshotProvider> {
        DeviceSnapshotBuilder(
            get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(),
        )
    }
    single { DeviceSyncManager(get(), get(), get(), get()) }
    single<SubscriptionVerifier> { BackendSubscriptionVerifier(get(), get()) }

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
    viewModel { SubscriptionViewModel(get(), useRealBilling) }
}
