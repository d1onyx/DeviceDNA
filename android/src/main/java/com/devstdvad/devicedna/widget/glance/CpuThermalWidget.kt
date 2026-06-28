package com.devstdvad.devicedna.widget.glance

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.unit.ColorProvider
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.data.widget.WidgetSnapshotCache
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler

class CpuThermalWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache(context).current()
        if (snapshot.lastUpdatedMillis == 0L || !snapshot.isPremium) WidgetRefreshScheduler.refreshNow(context)

        provideContent {
            if (!snapshot.isPremium) {
                LockedContent(context, "CPU · Thermal")
                return@provideContent
            }
            val usage = snapshot.cpuUsagePercent
            val usageText = if (usage < 0f) "—" else "${usage.toInt()}%"
            val usageColor = when {
                usage >= 85f -> WidgetColors.critical
                usage >= 60f -> WidgetColors.warning
                else -> WidgetColors.cpu
            }
            WidgetFrame(context, openRoute = "hardware") {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetHeader("CPU · Thermal", WidgetColors.cpu)
                        Spacer(GlanceModifier.height(6.dp))
                        BigValue(usageText, usageColor)
                        Caption("CPU load")
                        Spacer(GlanceModifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = (if (usage < 0f) 0f else usage / 100f).coerceIn(0f, 1f),
                            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                            color = ColorProvider(usageColor),
                            backgroundColor = ColorProvider(WidgetColors.track),
                        )
                    }
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Spacer(GlanceModifier.height(6.dp))
                        val cpuTemp = if (snapshot.cpuTempC > 0f) Formatters.formatCelsius(snapshot.cpuTempC) else "—"
                        MetricLine("CPU temp", cpuTemp, WidgetColors.textPrimary)
                        Spacer(GlanceModifier.height(2.dp))
                        val maxColor = if (snapshot.thermalMaxC >= 45f) WidgetColors.thermal else WidgetColors.textPrimary
                        val maxText = if (snapshot.thermalMaxC > 0f) Formatters.formatCelsius(snapshot.thermalMaxC) else "—"
                        MetricLine("Hottest zone", maxText, maxColor)
                        Spacer(GlanceModifier.height(2.dp))
                        Caption(updatedAgo(snapshot.lastUpdatedMillis))
                    }
                }
            }
        }
    }
}

class CpuThermalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CpuThermalWidget()
}
