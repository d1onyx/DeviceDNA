package com.devstdvad.devicedna.widget.glance

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.data.widget.WidgetSnapshotCache
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler

class BatteryDoctorWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache(context).current()
        if (snapshot.lastUpdatedMillis == 0L || !snapshot.isPremium) WidgetRefreshScheduler.refreshNow(context)

        provideContent {
            if (!snapshot.isPremium) {
                LockedContent(context, "Battery Doctor")
                return@provideContent
            }
            val wear = snapshot.batteryWearPercent
            val wearColor = when {
                wear < 0 -> WidgetColors.textMuted
                wear >= 85 -> WidgetColors.battery
                wear >= 70 -> WidgetColors.warning
                else -> WidgetColors.critical
            }
            val powerLabel: String?
            val powerValue: String?
            when {
                snapshot.batteryCharging && snapshot.batteryWatts > 0f -> {
                    powerLabel = "Charging"
                    powerValue = "%.1f W".format(snapshot.batteryWatts) +
                        if (snapshot.batteryChargeTimeMs > 0L) " · ${Formatters.formatUptimeMs(snapshot.batteryChargeTimeMs)}" else ""
                }
                snapshot.batteryWatts > 0f -> {
                    powerLabel = "Drain"
                    powerValue = "%.1f W".format(snapshot.batteryWatts)
                }
                else -> {
                    powerLabel = null
                    powerValue = null
                }
            }
            WidgetFrame(context, openRoute = "hardware") {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetHeader("Battery Doctor", WidgetColors.battery)
                        Spacer(GlanceModifier.height(6.dp))
                        BigValue(if (wear < 0) snapshot.batteryHealth.ifBlank { "—" } else "$wear%", wearColor)
                        Caption(if (wear < 0) "Battery health" else "Estimated health")
                        Spacer(GlanceModifier.height(8.dp))
                    }
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        // Health status is the BigValue when wear is unavailable; otherwise show it here.
                        if (wear >= 0) {
                            MetricLine("Health", snapshot.batteryHealth.ifBlank { "Unknown" }, healthColor(snapshot.batteryHealth))
                            Spacer(GlanceModifier.height(3.dp))
                        }
                        // Always show cycles, with a clear note when the OS doesn't expose them (< Android 14).
                        MetricLine(
                            "Cycles",
                            if (snapshot.batteryCycles >= 0) "${snapshot.batteryCycles}" else "Android 14+",
                            WidgetColors.textPrimary,
                        )
                        Spacer(GlanceModifier.height(3.dp))
                        if (powerLabel != null && powerValue != null) {
                            MetricLine(powerLabel, powerValue, WidgetColors.battery)
                            Spacer(GlanceModifier.height(3.dp))
                        }
                        MetricLine("Temp", Formatters.formatCelsius(snapshot.batteryTempC), WidgetColors.textPrimary)
                        Spacer(GlanceModifier.height(6.dp))
                        Caption(updatedAgo(snapshot.lastUpdatedMillis))
                    }
                }
            }
        }
    }
}

class BatteryDoctorWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryDoctorWidget()
}
