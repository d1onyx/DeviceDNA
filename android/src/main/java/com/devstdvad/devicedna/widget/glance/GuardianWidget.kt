package com.devstdvad.devicedna.widget.glance

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.devstdvad.devicedna.data.widget.WidgetSnapshotCache
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler

class GuardianWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache(context).current()
        if (snapshot.lastUpdatedMillis == 0L || !snapshot.isPremium) WidgetRefreshScheduler.refreshNow(context)

        provideContent {
            if (!snapshot.isPremium) {
                LockedContent(context, "Guardian")
                return@provideContent
            }
            val issues = snapshot.integrityIssues.split(", ").filter { it.isNotBlank() }
            val secure = issues.isEmpty()
            val color = if (secure) WidgetColors.battery else WidgetColors.critical
            WidgetFrame(context, openRoute = "system") {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetHeader("Guardian", WidgetColors.accent)
                        Spacer(GlanceModifier.height(6.dp))
                        BigValue(if (secure) "Secure" else "${issues.size} issue${if (issues.size == 1) "" else "s"}", color)
                        Caption(if (secure) "Device integrity OK" else "Attention needed")
                    }
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Spacer(GlanceModifier.height(10.dp))
                        if (secure) {
                            MetricLine("Root", "Not detected", WidgetColors.textPrimary)
                            Spacer(GlanceModifier.height(4.dp))
                            MetricLine("Integrity", "Verified", WidgetColors.battery)
                        } else {
                            issues.take(3).forEach { issue ->
                                Text(
                                    text = "• $issue",
                                    style = TextStyle(color = ColorProvider(WidgetColors.critical), fontSize = 13.sp),
                                    maxLines = 1,
                                )
                                Spacer(GlanceModifier.height(3.dp))
                            }
                        }
                        Spacer(GlanceModifier.height(8.dp))
                        Caption(updatedAgo(snapshot.lastUpdatedMillis))
                    }
                }
            }
        }
    }
}

class GuardianWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GuardianWidget()
}
