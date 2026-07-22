package com.devstdvad.devicedna.presentation.batteryintelligence

import com.devstdvad.devicedna.resources.stringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
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
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.core.design.component.StatusPill
import com.devstdvad.devicedna.data.batteryintelligence.BatteryIntelligenceHistoryStore
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.domain.batteryintelligence.ChargingSessionSummary
import com.devstdvad.devicedna.domain.batteryintelligence.buildChargingSessions
import com.devstdvad.devicedna.platform.PlatformInfo
import com.devstdvad.devicedna.presentation.common.SettingsFormatters
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryChargingPeriodsScreen(
    dayStartMillis: Long,
    settings: UserSettings = UserSettings(),
    onBackClick: () -> Unit,
    onChargingSessionClick: (ChargingSessionSummary) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    historyStore: BatteryIntelligenceHistoryStore = koinInject(),
) {
    val colors = AppTheme.colors
    val timeZone = TimeZone.currentSystemDefault()
    val snapshots by historyStore.snapshots.collectAsState(initial = emptyList())
    val sessions = buildChargingSessions(
        history = snapshots,
        dayStartMillis = dayStartMillis,
        timeZone = timeZone,
    )

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
                        text = stringRes("battery_periods_title"),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = dayStartMillis.formatDayRange(timeZone),
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
            if (!PlatformInfo.isIos && !settings.backgroundMonitoring) {
                item {
                    BatteryBackgroundMonitoringOffCard()
                }
            }

            if (sessions.isEmpty()) {
                item {
                    SectionCard {
                        Text(
                            text = stringRes("battery_intelligence_periods_empty"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            } else {
                items(sessions.size) { index ->
                    FullChargingSessionCard(
                        session = sessions[index],
                        settings = settings,
                        onClick = { onChargingSessionClick(sessions[index]) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FullChargingSessionCard(
    session: ChargingSessionSummary,
    settings: UserSettings,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    SectionCard(
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null, tint = colors.batteryColor)
                Column {
                    Text(
                        text = "${session.startLabel} - ${session.endLabel}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${session.durationLabel} • ${session.startLevelPercent}% → ${session.endLevelPercent}% (${formatDelta(session.levelDeltaPercent)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }
            StatusPill(
                status = if (session.needsAdvice) MetricStatus.Warning else MetricStatus.Normal,
                label = if (session.needsAdvice) {
                    stringRes("battery_intelligence_session_needs_advice")
                } else {
                    stringRes("battery_intelligence_session_good")
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("${session.averageWatts.formatWatts()} avg", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Text("${session.peakWatts.formatWatts()} peak", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Text(
                "${SettingsFormatters.formatTemperature(session.maxTemperatureCelsius, settings.temperatureUnit)} max",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
    }
}

private fun Long.formatDayRange(timeZone: TimeZone): String {
    val date = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date
    return "${date.monthShortName()} ${date.dayOfMonth} 00:00 - 24:00"
}

private fun kotlinx.datetime.LocalDate.monthShortName(): String = MONTHS[monthNumber - 1]

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatDelta(value: Int): String =
    if (value >= 0) "+$value%" else "$value%"

@Composable
private fun Float?.formatWatts(): String =
    this?.let { "${Formatters.twoDecimals(it)} W" } ?: stringRes("common_not_reported")
