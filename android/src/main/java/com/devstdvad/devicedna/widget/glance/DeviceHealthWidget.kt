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
import com.devstdvad.devicedna.data.widget.WidgetSnapshotCache
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler

class DeviceHealthWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache(context).current()
        if (snapshot.lastUpdatedMillis == 0L || !snapshot.isPremium) WidgetRefreshScheduler.refreshNow(context)

        provideContent {
            if (!snapshot.isPremium) {
                LockedContent(context, "Device Health")
                return@provideContent
            }
            val score = snapshot.healthOverall
            val color = scoreColor(score)
            WidgetFrame(context, openRoute = "dashboard") {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetHeader("Device Health", WidgetColors.accent)
                        Spacer(GlanceModifier.height(6.dp))
                        BigValue(if (score < 0) "—" else "$score", color)
                        Caption(healthVerdict(score))
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
                        Caption("Top insight")
                        Spacer(GlanceModifier.height(2.dp))
                        androidx.glance.text.Text(
                            text = snapshot.healthInsight.ifBlank { "All systems healthy" },
                            style = androidx.glance.text.TextStyle(
                                color = ColorProvider(severityColor(snapshot.healthSeverity)),
                                fontSize = 13.sp,
                            ),
                            maxLines = 2,
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Caption(updatedAgo(snapshot.lastUpdatedMillis))
                    }
                }
            }
        }
    }
}

private fun healthVerdict(score: Int): String = when {
    score < 0 -> "Tap to scan"
    score >= 80 -> "Healthy"
    score >= 55 -> "Needs attention"
    else -> "At risk"
}

class DeviceHealthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DeviceHealthWidget()
}
