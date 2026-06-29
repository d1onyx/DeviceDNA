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
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.data.widget.WidgetSnapshotCache
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler

class BatteryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache(context).current()
        // Re-check on first render; premium users can also refresh manually from the widget.
        if (snapshot.lastUpdatedMillis == 0L) WidgetRefreshScheduler.refreshNow(context)
        val ctx = localizedWidgetContext(context)

        provideContent {
            if (!snapshot.isPremium) {
                LockedContent(ctx, ctx.getString(R.string.widget_battery_title))
                return@provideContent
            }
            val level = snapshot.batteryLevel.coerceAtLeast(0)
            val color = when {
                level <= 15 -> WidgetColors.critical
                snapshot.batteryTempC >= 45f -> WidgetColors.warning
                else -> WidgetColors.battery
            }
            WidgetFrame(ctx, openRoute = "hardware/battery") {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetHeader(ctx.getString(R.string.widget_battery_title), WidgetColors.battery)
                        Spacer(GlanceModifier.height(6.dp))
                        BigValue("$level%", color)
                        Spacer(GlanceModifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = level / 100f,
                            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                            color = ColorProvider(color),
                            backgroundColor = ColorProvider(WidgetColors.track),
                        )
                    }
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Spacer(GlanceModifier.height(10.dp))
                        MetricLine(ctx.getString(R.string.widget_metric_temp), Formatters.formatCelsius(snapshot.batteryTempC), WidgetColors.textPrimary)
                        Spacer(GlanceModifier.height(4.dp))
                        MetricLine(
                            ctx.getString(R.string.widget_metric_status),
                            if (snapshot.batteryStatus.isBlank()) "—" else ctx.batteryStatusText(snapshot.batteryStatus),
                            WidgetColors.textPrimary,
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Caption(updatedAgo(ctx, snapshot.lastUpdatedMillis))
                    }
                }
            }
        }
    }
}

class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryWidget()
}
