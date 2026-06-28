package com.devstdvad.devicedna.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devstdvad.devicedna.data.alerts.SmartAlertsManager
import com.devstdvad.devicedna.data.widget.WidgetSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartAlertDebugReceiver : BroadcastReceiver(), KoinComponent {
    private val smartAlertsManager: SmartAlertsManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOW_BATTERY) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                smartAlertsManager.evaluateAndNotify(
                    WidgetSnapshot(
                        hasData = true,
                        batteryLevel = 10,
                        batteryCharging = false,
                    ),
                )
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_LOW_BATTERY = "com.devstdvad.devicedna.DEBUG_SMART_ALERT_LOW_BATTERY"
    }
}
