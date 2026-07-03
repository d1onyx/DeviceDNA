package com.devstdvad.devicedna.widget.glance

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.devstdvad.devicedna.MainActivity
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.core.design.DesignTokens
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionStore
import com.devstdvad.devicedna.data.widget.WidgetSnapshot
import com.devstdvad.devicedna.data.widget.WidgetSnapshotCache
import com.devstdvad.devicedna.widget.RefreshAction
import kotlinx.coroutines.flow.first

/** Static colors for widgets (no access to AppTheme outside the app process). */
object WidgetColors {
    val background = Color(0xFF111113)
    val surface = Color(0xFF1F1F23)
    val textPrimary = Color(0xFFFAFAFA)
    val textMuted = Color(0xFF9CA3AF)
    val accent = Color(DesignTokens.colorAccentDark)
    val battery = Color(DesignTokens.colorBatteryDark)
    val ram = Color(DesignTokens.colorRamDark)
    val storage = Color(DesignTokens.colorStorageDark)
    val cpu = Color(DesignTokens.colorCpuDark)
    val thermal = Color(DesignTokens.colorThermalDark)
    val warning = Color(DesignTokens.colorWarningDark)
    val critical = Color(DesignTokens.colorCriticalDark)
    val track = Color(0xFF3F3F46)
}

/** Intent that opens the app at a given internal route (handled by MainActivity). */
fun openAppIntent(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        // A unique data Uri per route is essential: PendingIntent equality (filterEquals)
        // ignores extras, so without it every widget would reuse the first-created
        // PendingIntent and open the same (wrong) screen. The route is also kept in an
        // extra for backwards compatibility; MainActivity reads the data Uri first.
        data = android.net.Uri.parse("$WIDGET_ROUTE_SCHEME://open/$route")
        putExtra(MainActivity.EXTRA_ROUTE, route)
    }

/** Scheme used to encode a widget deep-link route in the launch Intent's data Uri. */
const val WIDGET_ROUTE_SCHEME = "devicedna"

/** Common rounded card frame that opens [openRoute] on tap. */
@Composable
fun WidgetFrame(
    context: Context,
    openRoute: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(openAppIntent(context, openRoute))),
    ) {
        content()
    }
}

/** Header row: title + a tap-to-refresh icon button. */
@Composable
fun WidgetHeader(title: String, accent: Color) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = TextStyle(color = ColorProvider(accent), fontSize = 13.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.defaultWeight(),
        )
        // Circular button: a solid background makes the tap target register on all
        // launchers (a backgroundless box is ignored by some, e.g. MIUI), and
        // cornerRadius = half the size clips the press highlight to a circle.
        Box(
            modifier = GlanceModifier
                .size(34.dp)
                .cornerRadius(17.dp)
                .background(WidgetColors.surface)
                .clickable(actionRunCallback<RefreshAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_refresh),
                contentDescription = "Refresh",
                colorFilter = ColorFilter.tint(ColorProvider(WidgetColors.textMuted)),
                modifier = GlanceModifier.size(18.dp),
            )
        }
    }
}

@Composable
fun BigValue(value: String, color: Color = WidgetColors.textPrimary) {
    Text(text = value, style = TextStyle(color = ColorProvider(color), fontSize = 26.sp, fontWeight = FontWeight.Bold))
}

@Composable
fun Caption(text: String) {
    Text(text = text, style = TextStyle(color = ColorProvider(WidgetColors.textMuted), fontSize = 11.sp))
}

/**
 * "Updated HH:mm" caption from a wall-clock millis timestamp. Absolute time (not relative)
 * so each refresh visibly changes the value — widgets don't re-render to age relative text.
 * Localized via [context] so both the label and the time format follow the in-app language.
 */
fun updatedAgo(context: Context, lastUpdatedMillis: Long): String {
    if (lastUpdatedMillis <= 0L) return context.getString(R.string.widget_updating)
    val time = java.text.SimpleDateFormat("HH:mm", context.currentLocale())
        .format(java.util.Date(lastUpdatedMillis))
    return context.getString(R.string.widget_updated, time)
}

/** Color for a health score / insight severity. */
fun scoreColor(score: Int): Color = when {
    score < 0 -> WidgetColors.textMuted
    score >= 80 -> WidgetColors.battery
    score >= 55 -> WidgetColors.warning
    else -> WidgetColors.critical
}

fun severityColor(severity: String): Color = when (severity) {
    "Critical" -> WidgetColors.critical
    "Warning" -> WidgetColors.warning
    "Info" -> WidgetColors.accent
    else -> WidgetColors.battery
}

suspend fun loadWidgetSnapshotForRender(context: Context): WidgetSnapshot {
    val cache = WidgetSnapshotCache(context)
    val cached = cache.current()
    val nowMillis = System.currentTimeMillis()
    val widgetsUnlocked = SubscriptionStore(context).entitlements
        .first()
        .hasFeature(PremiumFeature.Widgets, nowMillis)

    if (!widgetsUnlocked) {
        val locked = WidgetSnapshot(isPremium = false, hasData = false, lastUpdatedMillis = nowMillis)
        if (cached.isPremium || cached.hasData) cache.save(locked)
        return locked
    }

    return if (cached.isPremium) cached else cached.copy(isPremium = true)
}

fun WidgetSnapshot.needsRefresh(hasRequiredData: Boolean): Boolean =
    isPremium && (!hasData || !hasRequiredData)

@Composable
fun WidgetStatusContent(
    context: Context,
    title: String,
    snapshot: WidgetSnapshot,
    hasRequiredData: Boolean,
    content: @Composable () -> Unit,
) {
    when {
        !snapshot.isPremium -> LockedContent(context, title)
        !hasRequiredData -> LoadingContent(context, title)
        else -> content()
    }
}

/** Color for a BatteryHealth enum name. */
fun healthColor(health: String): Color = when (health) {
    "Good" -> WidgetColors.battery
    "Overheat", "OverVoltage", "Dead", "Failure" -> WidgetColors.critical
    "Cold" -> WidgetColors.warning
    else -> WidgetColors.textPrimary
}

/** PowerManager.THERMAL_STATUS_* → color (label lives in `Context.thermalStatusText`). */
fun thermalStatusColor(status: Int): Color = when {
    status < 0 -> WidgetColors.textMuted
    status == 0 -> WidgetColors.battery
    status <= 2 -> WidgetColors.warning
    else -> WidgetColors.critical
}

@Composable
fun MetricLine(label: String, value: String, color: Color) {
    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = TextStyle(color = ColorProvider(WidgetColors.textMuted), fontSize = 12.sp),
            modifier = GlanceModifier.defaultWeight(),
        )
        Text(
            text = value,
            style = TextStyle(color = ColorProvider(color), fontSize = 14.sp, fontWeight = FontWeight.Medium),
        )
    }
}

/** Shown when the Widgets premium feature is not active. Tap opens the subscription screen. */
@Composable
fun LockedContent(context: Context, title: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .cornerRadius(16.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(openAppIntent(context, "subscription"))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🔒 $title",
                style = TextStyle(color = ColorProvider(WidgetColors.textPrimary), fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = context.getString(R.string.widget_unlock_premium),
                style = TextStyle(color = ColorProvider(WidgetColors.accent), fontSize = 12.sp),
            )
        }
    }
}

/** Shown for premium users while the worker is still collecting real metrics. */
@Composable
fun LoadingContent(context: Context, title: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.background)
            .cornerRadius(16.dp)
            .padding(14.dp)
            .clickable(actionRunCallback<RefreshAction>()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = TextStyle(color = ColorProvider(WidgetColors.textPrimary), fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                text = context.getString(R.string.widget_updating),
                style = TextStyle(color = ColorProvider(WidgetColors.textMuted), fontSize = 12.sp),
            )
        }
    }
}
