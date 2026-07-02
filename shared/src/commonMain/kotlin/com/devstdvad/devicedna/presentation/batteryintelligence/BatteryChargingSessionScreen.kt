package com.devstdvad.devicedna.presentation.batteryintelligence

import com.devstdvad.devicedna.resources.stringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.core.common.currentTimeMillis
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.core.design.component.StatusPill
import com.devstdvad.devicedna.data.batteryintelligence.BatteryHistorySnapshot
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.presentation.common.SettingsFormatters
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryChargingSessionScreen(
    sessionStartMillis: Long,
    sessionEndMillis: Long?,
    settings: UserSettings = UserSettings(),
    onBackClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    historyStore: BatteryIntelligenceHistoryStore = koinInject(),
) {
    val colors = AppTheme.colors
    val snapshots by historyStore.snapshots.collectAsState(initial = emptyList())
    val sessionSnapshots = snapshots
        .filter { snapshot ->
            snapshot.timestampMillis >= sessionStartMillis &&
                (sessionEndMillis == null || snapshot.timestampMillis <= sessionEndMillis)
        }
        .sortedBy { it.timestampMillis }
    val chargingSnapshots = sessionSnapshots.filter { it.isCharging || it.isPlugged }
    val first = chargingSnapshots.firstOrNull()
    val lastCharging = chargingSnapshots.lastOrNull()
    val disconnected = sessionEndMillis != null
    val watts = chargingSnapshots.mapNotNull { it.estimatedWatts }.filter { it > 0f }
    val timeZone = TimeZone.currentSystemDefault()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringRes("battery_session_title"),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = sessionRangeLabel(sessionStartMillis, sessionEndMillis, timeZone),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = colors.textSecondary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
        )

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 12.dp + contentPadding.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            StatusPill(
                                status = if (disconnected) MetricStatus.Normal else MetricStatus.PermissionRequired,
                                label = if (disconnected) stringRes("battery_session_finished") else stringRes("battery_session_ongoing"),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringRes("battery_session_summary"),
                                style = MaterialTheme.typography.displaySmall,
                                color = colors.textPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringRes("battery_session_summary_body"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(colors.batteryColor.copy(alpha = 0.12f))
                                .border(1.dp, colors.batteryColor.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = colors.batteryColor)
                        }
                    }
                }
            }

            item {
                SectionCard {
                    InfoRow(stringRes("battery_session_started"), first?.formatTime(timeZone) ?: stringRes("common_not_reported"), copyable = false)
                    InfoRow(stringRes("battery_session_ended"), sessionEndMillis?.formatTime(timeZone) ?: stringRes("battery_session_ongoing"), copyable = false)
                    InfoRow(stringRes("battery_session_duration"), formatDuration((sessionEndMillis ?: currentTimeMillis()) - sessionStartMillis), copyable = false)
                    InfoRow(stringRes("battery_session_level"), if (first != null && lastCharging != null) "${first.levelPercent}% → ${lastCharging.levelPercent}% (${formatDelta(lastCharging.levelPercent - first.levelPercent)})" else stringRes("common_not_reported"), copyable = false)
                    InfoRow(stringRes("battery_intelligence_average_speed"), watts.averageOrNull().formatWatts(), copyable = false)
                    InfoRow(stringRes("battery_intelligence_peak_speed"), watts.maxOrNull().formatWatts(), copyable = false)
                    InfoRow(
                        stringRes("battery_session_max_temp"),
                        chargingSnapshots.maxOfOrNull { it.temperatureCelsius }?.let {
                            SettingsFormatters.formatTemperature(it, settings.temperatureUnit)
                        } ?: stringRes("common_not_reported"),
                        copyable = false,
                        showDivider = false,
                    )
                }
            }

            item {
                SectionCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Speed, contentDescription = null, tint = colors.accent)
                        Text(
                            text = stringRes("battery_session_changes"),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    if (sessionSnapshots.isEmpty()) {
                        Text(
                            text = stringRes("battery_session_empty"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    } else {
                        sessionSnapshots.forEachIndexed { index, snapshot ->
                            SessionChangeRow(snapshot = snapshot, settings = settings, timeZone = timeZone)
                            if (index != sessionSnapshots.lastIndex) Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionChangeRow(
    snapshot: BatteryHistorySnapshot,
    settings: UserSettings,
    timeZone: TimeZone,
) {
    val colors = AppTheme.colors
    val charging = snapshot.isCharging || snapshot.isPlugged
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (charging) colors.success.copy(alpha = 0.14f) else colors.critical.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (charging) Icons.Outlined.Cable else Icons.Outlined.PowerOff,
                contentDescription = null,
                tint = if (charging) colors.success else colors.critical,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = snapshot.formatTime(timeZone),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${snapshot.status} • ${snapshot.source}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${snapshot.levelPercent}%", style = MaterialTheme.typography.titleMedium, color = colors.batteryColor)
            Text(
                "${snapshot.estimatedWatts.formatWatts()} • ${SettingsFormatters.formatTemperature(snapshot.temperatureCelsius, settings.temperatureUnit)}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}

private fun BatteryHistorySnapshot.formatTime(timeZone: TimeZone): String =
    timestampMillis.formatTime(timeZone)

private fun Long.formatTime(timeZone: TimeZone): String {
    val local = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone)
    return "${local.hour.twoDigits()}:${local.minute.twoDigits()}"
}

private fun sessionRangeLabel(startMillis: Long, endMillis: Long?, timeZone: TimeZone): String {
    val date = Instant.fromEpochMilliseconds(startMillis).toLocalDateTime(timeZone).date
    return "${date.monthShortName()} ${date.dayOfMonth} ${startMillis.formatTime(timeZone)} - ${endMillis?.formatTime(timeZone) ?: "ongoing"}"
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = (millis.coerceAtLeast(0L) / 60_000L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatDelta(value: Int): String =
    if (value >= 0) "+$value%" else "$value%"

private fun List<Float>.averageOrNull(): Float? =
    takeIf { it.isNotEmpty() }?.let { values -> values.sum() / values.size }

@Composable
private fun Float?.formatWatts(): String =
    this?.let { "${Formatters.twoDecimals(it)} W" } ?: stringRes("common_not_reported")

private fun kotlinx.datetime.LocalDate.monthShortName(): String = MONTHS[monthNumber - 1]

private fun Int.twoDigits(): String = if (this < 10) "0$this" else toString()

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
