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
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.data.widget.WidgetSnapshotCache
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler

class GuardianWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache(context).current()
        if (snapshot.lastUpdatedMillis == 0L || !snapshot.isPremium) WidgetRefreshScheduler.refreshNow(context)
        val ctx = localizedWidgetContext(context)

        provideContent {
            if (!snapshot.isPremium) {
                LockedContent(ctx, ctx.getString(R.string.widget_guardian_title))
                return@provideContent
            }
            val issues = snapshot.integrityIssues.split(", ").filter { it.isNotBlank() }
            val secure = issues.isEmpty()
            val color = if (secure) WidgetColors.battery else WidgetColors.critical
            WidgetFrame(ctx, openRoute = "hardware/device") {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetHeader(ctx.getString(R.string.widget_guardian_title), WidgetColors.accent)
                        Spacer(GlanceModifier.height(6.dp))
                        BigValue(
                            if (secure) ctx.getString(R.string.widget_secure)
                            else ctx.resources.getQuantityString(R.plurals.widget_issue_count, issues.size, issues.size),
                            color,
                        )
                        Caption(ctx.getString(if (secure) R.string.widget_device_integrity_ok else R.string.widget_attention_needed))
                    }
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Spacer(GlanceModifier.height(10.dp))
                        if (secure) {
                            MetricLine(ctx.getString(R.string.widget_metric_root), ctx.getString(R.string.widget_value_not_detected), WidgetColors.textPrimary)
                            Spacer(GlanceModifier.height(4.dp))
                            MetricLine(ctx.getString(R.string.widget_metric_integrity), ctx.getString(R.string.widget_value_verified), WidgetColors.battery)
                        } else {
                            issues.take(3).forEach { issue ->
                                Text(
                                    text = "• ${ctx.integrityIssueText(issue)}",
                                    style = TextStyle(color = ColorProvider(WidgetColors.critical), fontSize = 13.sp),
                                    maxLines = 1,
                                )
                                Spacer(GlanceModifier.height(3.dp))
                            }
                        }
                        Spacer(GlanceModifier.height(8.dp))
                        Caption(updatedAgo(ctx, snapshot.lastUpdatedMillis))
                    }
                }
            }
        }
    }
}

class GuardianWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GuardianWidget()
}
