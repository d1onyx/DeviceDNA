package com.devstdvad.devicedna.background

import com.devstdvad.devicedna.data.alerts.IosSmartAlertNotifier
import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.data.widget.IosWidgetBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The work a BGAppRefreshTask window executes — the iOS counterpart of Android's
 * WidgetRefreshWorker: refresh the widget snapshot and evaluate smart alerts. Task
 * *registration/scheduling* stays in Swift (BGTaskScheduler must be registered before
 * didFinishLaunching returns); Swift calls [run] and completes the BGTask from [onDone].
 *
 * Battery-history recording is intentionally NOT done here: Battery Intelligence is deactivated
 * on iOS (see navigation/NavRoutes.kt), so there is no history graph to feed. It stays active on
 * Android via its own WidgetRefreshWorker / BatteryMonitoringService.
 *
 * The whole pass is capped well under the ~30s budget iOS grants a refresh task.
 */
class IosBackgroundWorker(
    private val settingsStore: SettingsStore,
    private val widgetBridge: IosWidgetBridge,
    private val alertNotifier: IosSmartAlertNotifier,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun run(onDone: (Boolean) -> Unit) {
        scope.launch {
            val success = withTimeoutOrNull(20_000L) {
                runCatching {
                    val nowMillis = currentTimeMillis()
                    val settings = settingsStore.settings.first()

                    // 1. Widget snapshot + WidgetKit timeline reload.
                    val snapshot = widgetBridge.refresh()

                    // 2. Respect the user's background-monitoring switch. Widget refresh stays
                    // independent because WidgetKit may request a fresh timeline on its own.
                    if (settings.backgroundMonitoring) {
                        alertNotifier.evaluateAndNotify(snapshot, settings, nowMillis)
                    }
                }.isSuccess
            } ?: false
            onDone(success)
        }
    }
}
