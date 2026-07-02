package com.devstdvad.devicedna.data.alerts

import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.data.widget.WidgetSnapshot
import com.devstdvad.devicedna.resources.AppLanguage
import com.devstdvad.devicedna.resources.stringsFor
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSUserDefaults
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS Smart Alerts: evaluates the SAME shared [SmartAlertEvaluator] rules and raises local
 * notifications via UNUserNotificationCenter. Copy comes from the shared string catalog
 * (titleKey/bodyKey), so alert text is identical across platforms and locales.
 *
 * App Store review notes:
 *  • Authorization is requested lazily, the first time alerts actually fire — never at launch.
 *  • Notifications are user-serving device alerts (no marketing content), matching guideline 4.5.3.
 *  • A per-type cooldown prevents notification spam.
 */
class IosSmartAlertNotifier(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) {

    suspend fun requestAuthorization(): Boolean = suspendCancellableCoroutine { cont ->
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { granted, _ ->
            if (cont.isActive) cont.resume(granted)
        }
    }

    /** Evaluates the enabled alert rules against [snapshot] and posts due notifications. */
    suspend fun evaluateAndNotify(
        snapshot: WidgetSnapshot,
        settings: UserSettings,
        nowMillis: Long,
    ) {
        if (!settings.smartAlertsEnabled) return
        val enabled = settings.smartAlertTypes
            .mapNotNull { SmartAlertType.fromKey(it) }
            .toSet()
        val active = SmartAlertEvaluator.evaluate(snapshot, enabled)
        if (active.isEmpty()) return
        if (!requestAuthorization()) return

        val strings = stringsFor(AppLanguage.fromTag(settings.appLanguage))
        active.forEach { alert ->
            if (isCoolingDown(alert, nowMillis)) return@forEach
            post(
                id = "smart_alert_${alert.key}",
                title = strings[alert.titleKey],
                body = strings[alert.bodyKey],
                route = alert.route,
            )
            markNotified(alert, nowMillis)
        }
    }

    private fun post(id: String, title: String, body: String, route: String) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
            // The notification delegate reads this to deep-link via DeepLinkHolder.
            setUserInfo(mapOf<Any?, Any>("route" to route))
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = id,
            content = content,
            trigger = null, // deliver immediately
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request, withCompletionHandler = null)
    }

    private fun isCoolingDown(alert: SmartAlertType, nowMillis: Long): Boolean {
        val last = defaults.integerForKey(cooldownKey(alert))
        return nowMillis - last < COOLDOWN_MS
    }

    private fun markNotified(alert: SmartAlertType, nowMillis: Long) {
        defaults.setInteger(nowMillis, cooldownKey(alert))
    }

    private fun cooldownKey(alert: SmartAlertType) = "smart_alert_last_${alert.key}"

    private companion object {
        const val COOLDOWN_MS = 6L * 60L * 60L * 1000L // one alert per type per 6h
    }
}
