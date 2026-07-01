package com.devstdvad.devicedna.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devstdvad.devicedna.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Captures the moment the charger is plugged in or unplugged so Battery Intelligence records the
 * start/end of a charging session when background monitoring is enabled.
 *
 * [Intent.ACTION_POWER_CONNECTED] / [Intent.ACTION_POWER_DISCONNECTED] are exempt from the
 * Android 8+ implicit-broadcast restrictions (unlike `ACTION_BATTERY_CHANGED`), so they can be
 * declared in the manifest. We still gate them with the user's background monitoring setting before
 * delegating to [WidgetRefreshScheduler.refreshNow].
 */
class PowerConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED -> refreshIfBackgroundMonitoringEnabled(context)
        }
    }

    private fun refreshIfBackgroundMonitoringEnabled(context: Context) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val backgroundMonitoringEnabled = runCatching {
                    SettingsStore(appContext).settings.first().backgroundMonitoring
                }.getOrDefault(false)

                if (backgroundMonitoringEnabled) {
                    runCatching { WidgetRefreshScheduler.refreshNow(appContext) }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
