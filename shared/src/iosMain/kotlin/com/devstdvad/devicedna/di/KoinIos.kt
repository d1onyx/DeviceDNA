package com.devstdvad.devicedna.di

import com.devstdvad.devicedna.background.IosBackgroundWorker
import com.devstdvad.devicedna.data.account.LocalDataWiper
import com.devstdvad.devicedna.data.alerts.IosSmartAlertNotifier
import com.devstdvad.devicedna.data.auth.AuthGateway
import com.devstdvad.devicedna.data.auth.IosAuthGateway
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.batteryintelligence.IosBatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.core.AppReadiness
import com.devstdvad.devicedna.data.cfg.ConfigSync
import com.devstdvad.devicedna.data.cfg.SignatureCheck
import com.devstdvad.devicedna.data.cfg.SyncConfig
import com.devstdvad.devicedna.data.cfg.buildConfigSync
import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults
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
    val useRealBilling: Boolean,
    val syncBaseUrl: String,
    val appGroupId: String,
    val reloadWidgetTimelines: () -> Unit,
    // Remote config sync. [verifySignature] is provided by Swift; disabled while [cfgEnabled] is false.
    val cfgEnabled: Boolean = false,
    val cfgDocPath: String = "cfg/state",
    val verifySignature: (message: ByteArray, signature: ByteArray) -> Boolean = { _, _ -> false },
)

private fun iosModule(deps: IosAppDependencies, useDevBilling: Boolean) = module {
    // Auth + billing bridges (constructed in Swift, registered here)
    single<AuthGateway> { deps.authGateway }
    single { deps.authGateway }
    // Development artifacts unlock premium locally through a dev gateway (no App Store product
    // needed); production archives explicitly resolve to the real StoreKit bridge.
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
    single<PremiumEntitlementsStore> { IosEntitlementsStore(get()) }
    single<BatteryIntelligenceHistoryStore> { IosBatteryIntelligenceHistoryStore(deps.appGroupId) }
    single<SyncStateStore> { IosSyncStateStore() }

    // Account deletion: every on-device store holding user data, cleared after the account is gone.
    single {
        LocalDataWiper(
            listOf(
                get<SettingsStore>(),
                get<PremiumEntitlementsStore>(),
                get<BatteryIntelligenceHistoryStore>(),
                get<SyncStateStore>(),
                get<IosWidgetBridge>(),
            ),
        )
    }

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

    single<ConfigSync> {
        val verifier: SignatureCheck? =
            if (deps.cfgEnabled) SignatureCheck(deps.verifySignature) else null
        val config = SyncConfig(
            enabled = deps.cfgEnabled,
            documentPath = deps.cfgDocPath,
            appName = "cfg-sync",
            publicKeyBase64 = "",
        )
        val settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
        // Fail closed: cfgEnabled is false whenever GoogleService-Info-CfgSync.plist is absent from
        // the bundle, and without it the app must not run at all.
        buildConfigSync(config, settings, verifier, lockWhenUnavailable = true)
    }
    single { AppReadiness(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }

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
    single { IosBackgroundWorker(get(), get(), get()) }
}

/**
 * Starts Koin for the iOS host. Call once from the Swift AppDelegate before the first
 * Compose frame. Billing mode is explicit because CI uses an optimized Release configuration for
 * both sideloaded development artifacts and eventual App Store archives.
 */
fun initKoin(deps: IosAppDependencies): Koin {
    val useDevBilling = !deps.useRealBilling
    return startKoin {
        modules(iosModule(deps, useDevBilling), commonModule(useRealBilling = !useDevBilling))
    }.koin
}

/** Swift-friendly accessors for objects the host needs directly. */
object KoinBridge {
    private var koin: Koin? = null
    private var configSync: ConfigSync? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(deps: IosAppDependencies) {
        if (koin != null) return
        val instance = initKoin(deps)
        koin = instance

        // Seed remote config state, then keep a foreground subscription.
        val sync = instance.get<ConfigSync>()
        configSync = sync
        sync.onStartup()
        sync.attach(scope)


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
        // NOTE: Battery Intelligence is deactivated on iOS (see BatteryIntelligence tab gating in
        // navigation/NavRoutes.kt), so there is no app-wide battery-history recorder here. The
        // feature's code stays compiled but unwired on iOS; it remains fully active on Android.
    }

    /** Re-subscribe on foreground (scenePhase == .active). */
    fun attachConfigSync() {
        configSync?.attach(scope)
    }

    /** Drop the subscription on background. */
    fun detachConfigSync() {
        configSync?.detach()
    }

    fun backgroundWorker(): IosBackgroundWorker = requireNotNull(koin).get()
    fun settingsStore(): SettingsStore = requireNotNull(koin).get()
    fun authGateway(): IosAuthGateway = requireNotNull(koin).get()
    fun entitlementsStore(): PremiumEntitlementsStore = requireNotNull(koin).get()

    /**
     * Whether the resolved gateway is real StoreKit. Dev builds unlock premium locally, so the
     * Swift StoreKit bridge must not run: its entitlement sync would clear the dev entitlement.
     */
    fun usesRealBilling(): Boolean =
        requireNotNull(koin).get<SubscriptionBillingGateway>() is IosBillingGateway
}
