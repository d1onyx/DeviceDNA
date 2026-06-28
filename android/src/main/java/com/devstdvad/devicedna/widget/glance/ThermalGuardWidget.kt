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

class ThermalGuardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache(context).current()
        if (snapshot.lastUpdatedMillis == 0L || !snapshot.isPremium) WidgetRefreshScheduler.refreshNow(context)

        provideContent {
            if (!snapshot.isPremium) {
                LockedContent(context, "Thermal Guard")
                return@provideContent
            }
            val statusColor = thermalStatusColor(snapshot.thermalStatus)
            val statusLabel = thermalStatusLabel(snapshot.thermalStatus)
            val peak = if (snapshot.thermalMaxC > 0f) Formatters.formatCelsius(snapshot.thermalMaxC)
            else Formatters.formatCelsius(snapshot.batteryTempC)
            WidgetFrame(context, openRoute = "hardware") {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetHeader("Thermal Guard", WidgetColors.thermal)
                        Spacer(GlanceModifier.height(6.dp))
                        BigValue(statusLabel, statusColor)
                        Caption("Peak $peak")
                    }
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Spacer(GlanceModifier.height(10.dp))
                        if (snapshot.cpuMaxFreqMhz > 0 && snapshot.cpuCurFreqMhz > 0) {
                            val throttled = snapshot.cpuCurFreqMhz < snapshot.cpuMaxFreqMhz * 0.6f
                            MetricLine(
                                "CPU clock",
                                "${snapshot.cpuCurFreqMhz} / ${snapshot.cpuMaxFreqMhz} MHz",
                                if (throttled) WidgetColors.warning else WidgetColors.textPrimary,
                            )
                            Spacer(GlanceModifier.height(4.dp))
                        }
                        MetricLine(
                            "Status",
                            if (snapshot.thermalStatus >= 3) "Throttling now" else "Stable",
                            statusColor,
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Caption(updatedAgo(snapshot.lastUpdatedMillis))
                    }
                }
            }
        }
    }
}

class ThermalGuardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ThermalGuardWidget()
}
