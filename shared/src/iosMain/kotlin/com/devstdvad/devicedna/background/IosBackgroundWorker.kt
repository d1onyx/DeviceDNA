package com.devstdvad.devicedna.background

import com.devstdvad.devicedna.core.common.getOrNull
import com.devstdvad.devicedna.data.alerts.IosSmartAlertNotifier
import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.devstdvad.devicedna.data.batteryintelligence.BatteryHistoryTracker
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.data.subscription.PremiumEntitlementsStore
import com.devstdvad.devicedna.data.widget.IosWidgetBridge
import com.devstdvad.devicedna.domain.repository.BatteryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The work a BGAppRefreshTask window executes — the iOS counterpart of Android's
 * WidgetRefreshWorker: record a battery-history sample, refresh the widget snapshot,
 * evaluate smart alerts. Task *registration/scheduling* stays in Swift (BGTaskScheduler
 * must be registered before didFinishLaunching returns); Swift calls [run] and completes
 * the BGTask from [onDone].
 *
 * The whole pass is capped well under the ~30s budget iOS grants a refresh task.
 */
class IosBackgroundWorker(
    private val batteryRepository: BatteryRepository,
    private val historyStore: BatteryIntelligenceHistoryStore,
    private val settingsStore: SettingsStore,
    private val widgetBridge: IosWidgetBridge,
    private val alertNotifier: IosSmartAlertNotifier,
    private val historyTracker: BatteryHistoryTracker,
    private val entitlementsStore: PremiumEntitlementsStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun run(onDone: (Boolean) -> Unit) {
        scope.launch {
            val success = withTimeoutOrNull(20_000L) {
                runCatching {
                    val nowMillis = currentTimeMillis()
                    val settings = settingsStore.settings.first()

                    // 1. Battery-history sample. Routed through the shared tracker so the premium +
                    // charging-tracking gate matches the screen VM and Android's boundary recorder.
                    val entitlements = entitlementsStore.entitlements.first()
                    val trackingEnabled = historyStore.chargingTrackingEnabled.first()
                    val battery = batteryRepository.getBatterySnapshot().getOrNull()
                    historyTracker.onBatterySample(entitlements, battery, trackingEnabled, nowMillis)

                    // 2. Widget snapshot + WidgetKit timeline reload.
                    val snapshot = widgetBridge.refresh()

                    // 3. Smart alerts on the fresh metrics.
                    alertNotifier.evaluateAndNotify(snapshot, settings, nowMillis)
                }.isSuccess
            } ?: false
            onDone(success)
        }
    }
}
