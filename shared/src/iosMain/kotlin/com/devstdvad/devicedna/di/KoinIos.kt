package com.devstdvad.devicedna.di

import com.devstdvad.devicedna.background.IosBackgroundWorker
import com.devstdvad.devicedna.data.alerts.IosSmartAlertNotifier
import com.devstdvad.devicedna.data.auth.AuthGateway
import com.devstdvad.devicedna.data.auth.IosAuthGateway
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.batteryintelligence.IosBatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.settings.IosSettingsStore
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.data.source.IosAppsRepository
import com.devstdvad.devicedna.data.source.IosBatteryRepository
import com.devstdvad.devicedna.data.source.IosCameraRepository
import com.devstdvad.devicedna.data.source.IosCpuRepository
import com.devstdvad.devicedna.data.source.IosDeviceRepository
import com.devstdvad.devicedna.data.source.IosDisplayRepository
import com.devstdvad.devicedna.data.source.IosNetworkRepository
import com.devstdvad.devicedna.data.source.IosRamStorageRepository
import com.devstdvad.devicedna.data.source.IosSensorRepository
import com.devstdvad.devicedna.data.source.IosSystemRepository
import com.devstdvad.devicedna.data.source.IosThermalRepository
import com.devstdvad.devicedna.data.subscription.IosBillingGateway
import com.devstdvad.devicedna.data.subscription.IosEntitlementsStore
import com.devstdvad.devicedna.data.subscription.PremiumEntitlementsStore
import com.devstdvad.devicedna.data.subscription.SubscriptionBillingGateway
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.data.sync.IosSyncFactory
import com.devstdvad.devicedna.data.sync.IosSyncStateStore
import com.devstdvad.devicedna.data.sync.SyncStateStore
import com.devstdvad.devicedna.data.widget.IosWidgetBridge
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
import com.devstdvad.devicedna.core.feedback.HapticManager
import com.devstdvad.devicedna.core.feedback.SoundManager
import com.devstdvad.devicedna.platform.FileImporter
import com.devstdvad.devicedna.platform.FileSharer
import com.devstdvad.devicedna.platform.IosFileImporter
import com.devstdvad.devicedna.platform.IosFileSharer
import com.devstdvad.devicedna.platform.IosHapticManager
import com.devstdvad.devicedna.platform.IosSoundManager
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** App Group shared between the app, the WidgetKit extension and BGTask work. */
const val APP_GROUP_ID = "group.com.devstdvad.devicedna"

/**
 * Everything the Swift host must hand over at startup (auth/billing bridges and the
 * WidgetKit reload hook are Swift-only APIs the Kotlin side cannot construct itself).
 */
class IosAppDependencies(
    val authGateway: IosAuthGateway,
    val billingGateway: IosBillingGateway,
    val syncBaseUrl: String,
    val reloadWidgetTimelines: () -> Unit,
)

private fun iosModule(deps: IosAppDependencies) = module {
    // Auth + billing bridges (constructed in Swift, registered here)
    single<AuthGateway> { deps.authGateway }
    single { deps.authGateway }
    single<SubscriptionBillingGateway> { deps.billingGateway }

    // Repositories (iOS data layer)
    single { IosRamStorageRepository() }
    single<DeviceRepository> { IosDeviceRepository() }
    single<SystemRepository> { IosSystemRepository() }
    single<CpuRepository> { IosCpuRepository() }
    single<BatteryRepository> { IosBatteryRepository() }
    single<RamRepository> { get<IosRamStorageRepository>() }
    single<StorageRepository> { get<IosRamStorageRepository>() }
    single { IosNetworkRepository() }
    single<NetworkRepository> { get<IosNetworkRepository>() }
    single<ConnectivityRepository> { get<IosNetworkRepository>() }
    single<DisplayRepository> { IosDisplayRepository() }
    single<CameraRepository> { IosCameraRepository() }
    single<ThermalRepository> { IosThermalRepository() }
    single<SensorRepository> { IosSensorRepository() }
    single<AppsRepository> { IosAppsRepository() }

    // Persistence
    single<SettingsStore> { IosSettingsStore() }
    single<PremiumEntitlementsStore> { IosEntitlementsStore() }
    single<BatteryIntelligenceHistoryStore> { IosBatteryIntelligenceHistoryStore() }
    single<SyncStateStore> { IosSyncStateStore() }

    // Platform services
    single<FileSharer> { IosFileSharer() }
    single<FileImporter> { IosFileImporter() }
    single<HapticManager> { IosHapticManager() }
    single<SoundManager> { IosSoundManager() }

    // Sync transport (Ktor Darwin engine)
    single { IosSyncFactory.create(deps.syncBaseUrl) }

    // Subscription orchestration: StoreKit verified the purchase, so the backend is NOT
    // authoritative on iOS (it has no App Store verification endpoint yet) — the Swift
    // billing bridge re-pushes Transaction.currentEntitlements on launch instead.
    single {
        SubscriptionRepository(
            store = get(),
            billingGateway = get(),
            verifier = get(),
            devUsesBackend = false,
            serverAuthoritative = false,
        )
    }

    // Widgets / alerts / background
    single { IosSmartAlertNotifier() }
    single {
        IosWidgetBridge(
            observeBattery = get(),
            observeRam = get(),
            getStorage = get(),
            getThermal = get(),
            getDevice = get(),
            getHealth = get(),
            entitlementsStore = get(),
            reloadWidgetTimelines = deps.reloadWidgetTimelines,
        )
    }
    single { IosBackgroundWorker(get(), get(), get(), get(), get()) }
}

/**
 * Starts Koin for the iOS host. Call once from the Swift AppDelegate before the first
 * Compose frame. `useRealBilling = true`: App Store builds always use real StoreKit.
 */
fun initKoin(deps: IosAppDependencies): Koin =
    startKoin {
        modules(iosModule(deps), commonModule(useRealBilling = true))
    }.koin

/** Swift-friendly accessors for objects the host needs directly. */
object KoinBridge {
    private var koin: Koin? = null

    fun start(deps: IosAppDependencies) {
        if (koin == null) koin = initKoin(deps)
    }

    fun backgroundWorker(): IosBackgroundWorker = requireNotNull(koin).get()
    fun widgetBridge(): IosWidgetBridge = requireNotNull(koin).get()
    fun settingsStore(): SettingsStore = requireNotNull(koin).get()
    fun authGateway(): IosAuthGateway = requireNotNull(koin).get()
    fun entitlementsStore(): PremiumEntitlementsStore = requireNotNull(koin).get()
}
