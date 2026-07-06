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
import com.devstdvad.devicedna.data.subscription.IosDevBillingGateway
import com.devstdvad.devicedna.data.subscription.IosEntitlementsStore
import com.devstdvad.devicedna.data.subscription.PremiumEntitlementsStore
import com.devstdvad.devicedna.data.subscription.SubscriptionBillingGateway
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Everything the Swift host must hand over at startup (auth/billing bridges and the
 * WidgetKit reload hook are Swift-only APIs the Kotlin side cannot construct itself).
 */
class IosAppDependencies(
    val authGateway: IosAuthGateway,
    val billingGateway: IosBillingGateway,
    val syncBaseUrl: String,
    val appGroupId: String,
    val reloadWidgetTimelines: () -> Unit,
)

private fun iosModule(deps: IosAppDependencies, useDevBilling: Boolean) = module {
    // Auth + billing bridges (constructed in Swift, registered here)
    single<AuthGateway> { deps.authGateway }
    single { deps.authGateway }
    // Debug builds unlock premium locally through a dev gateway (no App Store sandbox account
    // needed); release builds always use the real StoreKit bridge.
    single<SubscriptionBillingGateway> {
        if (useDevBilling) IosDevBillingGateway() else deps.billingGateway
    }

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
    single<BatteryIntelligenceHistoryStore> { IosBatteryIntelligenceHistoryStore(deps.appGroupId) }
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
            appGroupId = deps.appGroupId,
        )
    }
    single { IosBackgroundWorker(get(), get(), get(), get(), get()) }
}

/**
 * Starts Koin for the iOS host. Call once from the Swift AppDelegate before the first
 * Compose frame. Debug binaries get a local dev unlock (and the dev subscription controls);
 * release/App Store builds always use real StoreKit.
 */
@OptIn(ExperimentalNativeApi::class)
fun initKoin(deps: IosAppDependencies): Koin {
    val useDevBilling = Platform.isDebugBinary
    return startKoin {
        modules(iosModule(deps, useDevBilling), commonModule(useRealBilling = !useDevBilling))
    }.koin
}

/** Swift-friendly accessors for objects the host needs directly. */
object KoinBridge {
    private var koin: Koin? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(deps: IosAppDependencies) {
        if (koin != null) return
        val instance = initKoin(deps)
        koin = instance

        // Refresh WidgetKit timelines the moment premium status changes (purchase, restore, dev
        // activate), regardless of which screen is open — otherwise widgets only unlock/lock on
        // the next cold launch, background transition, or the ~15-min BGAppRefreshTask.
        val entitlementsStore = instance.get<PremiumEntitlementsStore>()
        val widgetBridge = instance.get<IosWidgetBridge>()
        scope.launch {
            entitlementsStore.entitlements
                .map { it.hasFeature(PremiumFeature.Widgets) }
                .distinctUntilChanged()
                .drop(1)
                .collect { widgetBridge.refresh() }
        }
    }

    fun backgroundWorker(): IosBackgroundWorker = requireNotNull(koin).get()
    fun settingsStore(): SettingsStore = requireNotNull(koin).get()
    fun authGateway(): IosAuthGateway = requireNotNull(koin).get()
    fun entitlementsStore(): PremiumEntitlementsStore = requireNotNull(koin).get()
}
