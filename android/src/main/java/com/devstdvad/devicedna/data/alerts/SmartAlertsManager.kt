package com.devstdvad.devicedna.data.alerts

import com.devstdvad.devicedna.core.notification.SmartAlertNotifier
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.data.widget.WidgetSnapshot
import kotlinx.coroutines.flow.first

/**
 * Evaluates Smart Alerts against a fresh metrics snapshot and posts/clears notifications,
 * gated by premium + user settings, with per-alert cooldown to avoid spamming.
 */
class SmartAlertsManager(
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsStore: SettingsStore,
    private val stateStore: SmartAlertsStateStore,
    private val notifier: SmartAlertNotifier,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun evaluateAndNotify(snapshot: WidgetSnapshot) {
        val unlocked = subscriptionRepository.entitlements.first().hasFeature(PremiumFeature.SmartAlerts)
        if (!unlocked) return

        val settings = settingsStore.settings.first()
        if (!settings.smartAlertsEnabled) return
        val enabledKeys = settings.smartAlertTypes

        val nowMs = now()
        for (type in SmartAlertType.entries) {
            val active = type.key in enabledKeys && SmartAlertEvaluator.isActive(type, snapshot)
            val st = stateStore.state(type)
            when {
                SmartAlertsStateStore.shouldNotify(active, st.wasActive, st.lastNotifiedMillis, nowMs) -> {
                    notifier.notify(type)
                    stateStore.update(type, nowMs, wasActive = true)
                }
                !active && st.wasActive -> {
                    notifier.cancel(type)
                    stateStore.update(type, st.lastNotifiedMillis, wasActive = false)
                }
                // active within cooldown, or already-inactive: no change
            }
        }
    }
}
