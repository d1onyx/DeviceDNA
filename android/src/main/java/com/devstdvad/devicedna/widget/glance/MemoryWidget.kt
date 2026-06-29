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

class MemoryWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache(context).current()
        if (snapshot.lastUpdatedMillis == 0L || !snapshot.isPremium) WidgetRefreshScheduler.refreshNow(context)
        val ctx = localizedWidgetContext(context)

        provideContent {
            if (!snapshot.isPremium) {
                LockedContent(ctx, ctx.getString(R.string.widget_memory_title))
                return@provideContent
            }
            WidgetFrame(ctx, openRoute = "dashboard") {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        WidgetHeader(ctx.getString(R.string.widget_memory_title), WidgetColors.ram)
                        Spacer(GlanceModifier.height(10.dp))
                        MetricLine(
                            ctx.getString(R.string.widget_metric_ram),
                            "${Formatters.formatPercent(snapshot.ramUsedPercent)} · " +
                                "${Formatters.formatBytesShort(snapshot.ramUsedBytes)}/${Formatters.formatBytesShort(snapshot.ramTotalBytes)}",
                            WidgetColors.ram,
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = snapshot.ramUsedPercent.coerceIn(0f, 1f),
                            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                            color = ColorProvider(WidgetColors.ram),
                            backgroundColor = ColorProvider(WidgetColors.track),
                        )
                    }
                    Column(modifier = GlanceModifier.fillMaxWidth()) {
                        Spacer(GlanceModifier.height(12.dp))
                        MetricLine(
                            ctx.getString(R.string.widget_metric_storage),
                            "${Formatters.formatPercent(snapshot.storageUsedPercent)} · " +
                                "${Formatters.formatBytesShort(snapshot.storageUsedBytes)}/${Formatters.formatBytesShort(snapshot.storageTotalBytes)}",
                            WidgetColors.storage,
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = snapshot.storageUsedPercent.coerceIn(0f, 1f),
                            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                            color = ColorProvider(WidgetColors.storage),
                            backgroundColor = ColorProvider(WidgetColors.track),
                        )
                        Spacer(GlanceModifier.height(10.dp))
                        Caption(updatedAgo(ctx, snapshot.lastUpdatedMillis))
                    }
                }
            }
        }
    }
}

class MemoryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MemoryWidget()
}
