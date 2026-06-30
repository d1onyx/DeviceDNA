package com.devstdvad.devicedna.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Captures the moment the charger is plugged in or unplugged so Battery Intelligence records the
 * start/end of a charging session even while the app is closed and the device is in Doze.
 *
 * [Intent.ACTION_POWER_CONNECTED] / [Intent.ACTION_POWER_DISCONNECTED] are exempt from the
 * Android 8+ implicit-broadcast restrictions (unlike `ACTION_BATTERY_CHANGED`), so they can be
 * declared in the manifest. We delegate to [WidgetRefreshScheduler.refreshNow], whose expedited
 * worker records a battery snapshot (gated on the Battery Intelligence tracking toggle) and
 * refreshes the widgets — reusing the same path as the periodic refresh.
 */
class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED -> WidgetRefreshScheduler.refreshNow(context)
        }
    }
}
