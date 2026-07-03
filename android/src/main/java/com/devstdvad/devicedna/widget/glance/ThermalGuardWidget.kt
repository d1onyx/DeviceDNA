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
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler

class ThermalGuardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshotForRender(context)
        val hasRequiredData = snapshot.hasThermalData || snapshot.thermalStatus >= 0 || snapshot.hasBatteryData
        if (snapshot.lastUpdatedMillis == 0L || snapshot.needsRefresh(hasRequiredData)) {
            WidgetRefreshScheduler.refreshNow(context)
        }
        val ctx = localizedWidgetContext(context)
        val title = ctx.getString(R.string.widget_thermal_guard_title)

        provideContent {
            WidgetStatusContent(ctx, title, snapshot, hasRequiredData) {
                val statusColor = thermalStatusColor(snapshot.thermalStatus)
                val statusLabel = ctx.thermalStatusText(snapshot.thermalStatus)
                val peak = if (snapshot.thermalMaxC > 0f) Formatters.formatCelsius(snapshot.thermalMaxC)
                else Formatters.formatCelsius(snapshot.batteryTempC)
                WidgetFrame(ctx, openRoute = "hardware/thermal") {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            WidgetHeader(ctx.getString(R.string.widget_thermal_guard_title), WidgetColors.thermal)
                            Spacer(GlanceModifier.height(6.dp))
                            BigValue(statusLabel, statusColor)
                            Caption(ctx.getString(R.string.widget_peak, peak))
                        }
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            Spacer(GlanceModifier.height(10.dp))
                            if (snapshot.cpuMaxFreqMhz > 0 && snapshot.cpuCurFreqMhz > 0) {
                                val throttled = snapshot.cpuCurFreqMhz < snapshot.cpuMaxFreqMhz * 0.6f
                                MetricLine(
                                    ctx.getString(R.string.widget_metric_cpu_clock),
                                    "${snapshot.cpuCurFreqMhz} / ${snapshot.cpuMaxFreqMhz} MHz",
                                    if (throttled) WidgetColors.warning else WidgetColors.textPrimary,
                                )
                                Spacer(GlanceModifier.height(4.dp))
                            }
                            MetricLine(
                                ctx.getString(R.string.widget_metric_status),
                                ctx.getString(if (snapshot.thermalStatus >= 3) R.string.widget_throttling_now else R.string.widget_stable),
                                statusColor,
                            )
                            Spacer(GlanceModifier.height(4.dp))
                            Caption(updatedAgo(ctx, snapshot.lastUpdatedMillis))
                        }
                    }
                }
            }
        }
    }
}

class ThermalGuardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ThermalGuardWidget()
}
