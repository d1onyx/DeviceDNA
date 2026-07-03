package com.devstdvad.devicedna.widget.glance

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler

class DeviceHealthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadWidgetSnapshotForRender(context)
        val hasRequiredData = snapshot.hasHealthData
        if (snapshot.lastUpdatedMillis == 0L || snapshot.needsRefresh(hasRequiredData)) {
            WidgetRefreshScheduler.refreshNow(context)
        }
        val ctx = localizedWidgetContext(context)
        val title = ctx.getString(R.string.widget_device_health_title)

        provideContent {
            WidgetStatusContent(ctx, title, snapshot, hasRequiredData) {
                val score = snapshot.healthOverall
                val color = scoreColor(score)
                WidgetFrame(ctx, openRoute = "dashboard") {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            WidgetHeader(ctx.getString(R.string.widget_device_health_title), WidgetColors.accent)
                            Spacer(GlanceModifier.height(6.dp))
                            BigValue(if (score < 0) "—" else "$score", color)
                            Caption(ctx.healthVerdict(score))
                            Spacer(GlanceModifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = (if (score < 0) 0 else score) / 100f,
                                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                                color = ColorProvider(color),
                                backgroundColor = ColorProvider(WidgetColors.track),
                            )
                        }
                        Column(modifier = GlanceModifier.fillMaxWidth()) {
                            Spacer(GlanceModifier.height(10.dp))
                            Caption(ctx.getString(R.string.widget_caption_top_insight))
                            Spacer(GlanceModifier.height(2.dp))
                            androidx.glance.text.Text(
                                text = snapshot.healthInsight.ifBlank { ctx.getString(R.string.widget_all_systems_healthy) },
                                style = androidx.glance.text.TextStyle(
                                    color = ColorProvider(severityColor(snapshot.healthSeverity)),
                                    fontSize = 13.sp,
                                ),
                                maxLines = 2,
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

private fun Context.healthVerdict(score: Int): String = getString(
    when {
        score < 0 -> R.string.widget_tap_to_scan
        score >= 80 -> R.string.widget_verdict_healthy
        score >= 55 -> R.string.widget_verdict_needs_attention
        else -> R.string.widget_verdict_at_risk
    },
)

class DeviceHealthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeviceHealthWidget()
}
