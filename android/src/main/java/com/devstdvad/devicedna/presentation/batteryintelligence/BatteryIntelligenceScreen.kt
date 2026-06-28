package com.devstdvad.devicedna.presentation.batteryintelligence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.AccentCard
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.MetricCard
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.core.design.component.StatusPill
import com.devstdvad.devicedna.core.feedback.LocalAppFeedback
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun BatteryIntelligenceScreen(
    viewModel: BatteryIntelligenceViewModel = koinViewModel(),
    onSubscribeClick: () -> Unit = {},
    onChargingSessionClick: (ChargingSessionSummary) -> Unit = {},
    onShowAllChargingSessionsClick: (Long) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) {
        LoadingScreen()
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.isPremiumUnlocked) {
            item {
                PremiumLockedBatteryIntelligence(
                    onSubscribeClick = onSubscribeClick,
                )
            }
            return@LazyColumn
        }

        val report = state.intelligence
        if (report == null) {
            item {
                LoadingScreen(message = state.error ?: stringResource(R.string.battery_intelligence_unavailable))
            }
            return@LazyColumn
        }

        item {
            BatteryIntelligenceHero(report = report)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = stringResource(R.string.battery_intelligence_health_score),
                    primaryValue = "${report.healthScore}",
                    secondaryValue = stringResource(R.string.battery_intelligence_health_score_hint),
                    icon = Icons.Outlined.HealthAndSafety,
                    progress = report.healthScore / 100f,
                    status = if (report.healthScore >= 80) MetricStatus.Normal else MetricStatus.Warning,
                    accentColor = colors.batteryColor,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = stringResource(R.string.battery_intelligence_degradation),
                    primaryValue = "${report.degradationRiskPercent}%",
                    secondaryValue = report.degradationRiskLabel,
                    icon = Icons.Outlined.Timeline,
                    progress = report.degradationRiskPercent / 100f,
                    status = if (report.degradationRiskPercent >= 45) MetricStatus.Warning else MetricStatus.Normal,
                    accentColor = colors.warning,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            SectionCard {
                SectionTitle(Icons.Outlined.Speed, stringResource(R.string.battery_intelligence_charge_speed))
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = stringResource(R.string.battery_intelligence_current_speed),
                    value = report.chargeSpeed.currentWatts.formatWatts(),
                    copyable = false,
                )
                InfoRow(
                    label = stringResource(R.string.battery_intelligence_average_speed),
                    value = report.chargeSpeed.averageWatts.formatWatts(),
                    copyable = false,
                )
                InfoRow(
                    label = stringResource(R.string.battery_intelligence_peak_speed),
                    value = report.chargeSpeed.peakWatts.formatWatts(),
                    copyable = false,
                )
                InfoRow(
                    label = stringResource(R.string.battery_intelligence_charge_rate),
                    value = report.chargeSpeed.percentPerHour?.let { "%.1f%%/h".format(it) } ?: stringResource(R.string.common_not_reported),
                    copyable = false,
                )
                InfoRow(
                    label = stringResource(R.string.battery_intelligence_sessions),
                    value = report.chargeSpeed.chargingSessions.toString(),
                    copyable = false,
                    showDivider = false,
                )
            }
        }

        item {
            SectionCard {
                DailyChargingTimeline(
                    report = report,
                    onPreviousDay = viewModel::goToPreviousDay,
                    onNextDay = viewModel::goToNextDay,
                    onChargingSessionClick = onChargingSessionClick,
                    onShowAllChargingSessionsClick = onShowAllChargingSessionsClick,
                )
            }
        }

        item {
            SectionCard {
                SectionTitle(Icons.Outlined.Lightbulb, stringResource(R.string.battery_intelligence_charging_advice))
                Spacer(Modifier.height(10.dp))
                report.chargingAdvice.forEachIndexed { index, advice ->
                    AdviceRow(number = index + 1, text = advice)
                    if (index != report.chargingAdvice.lastIndex) Spacer(Modifier.height(8.dp))
                }
            }
        }

        item {
            SectionCard {
                SectionTitle(Icons.Outlined.History, stringResource(R.string.battery_intelligence_cycle_history))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        if (report.cycleStats.source == BatteryCycleSource.SystemReported) {
                            R.string.battery_intelligence_cycles_system_note
                        } else {
                            R.string.battery_intelligence_cycles_local_note
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                InfoRow(
                    label = stringResource(R.string.battery_intelligence_current_cycles),
                    value = report.cycleStats.currentCycles?.let { cycles ->
                        stringResource(
                            if (report.cycleStats.source == BatteryCycleSource.LocalEstimate) {
                                R.string.battery_intelligence_estimated_cycles_value
                            } else {
                                R.string.battery_intelligence_cycles_value
                            },
                            cycles,
                        )
                    } ?: stringResource(R.string.common_not_reported),
                    copyable = false,
                )
                if (report.cycleStats.source == BatteryCycleSource.LocalEstimate) {
                    InfoRow(
                        label = stringResource(R.string.battery_intelligence_next_cycle_progress),
                        value = "${report.cycleStats.partialCyclePercent}%",
                        copyable = false,
                    )
                } else {
                    InfoRow(
                        label = stringResource(R.string.battery_intelligence_cycle_delta),
                        value = report.cycleStats.cycleDelta?.let { "+$it" } ?: stringResource(R.string.common_not_enough_data),
                        copyable = false,
                    )
                }
                InfoRow(
                    label = stringResource(R.string.battery_intelligence_tracked_samples),
                    value = report.cycleStats.trackedSamples.toString(),
                    copyable = false,
                    showDivider = false,
                )
                Spacer(Modifier.height(12.dp))
                if (report.cycleHistory.isEmpty()) {
                    Text(
                        text = stringResource(R.string.battery_intelligence_cycles_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                } else {
                    report.cycleHistory.forEachIndexed { index, point ->
                        InfoRow(
                            label = point.label,
                            value = stringResource(R.string.battery_intelligence_cycles_value, point.cycles),
                            copyable = false,
                            showDivider = index != report.cycleHistory.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyChargingTimeline(
    report: BatteryIntelligenceReport,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onChargingSessionClick: (ChargingSessionSummary) -> Unit,
    onShowAllChargingSessionsClick: (Long) -> Unit,
) {
    val colors = AppTheme.colors
    SectionTitle(Icons.Outlined.BatteryChargingFull, stringResource(R.string.battery_intelligence_charging_history))
    Spacer(Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousDay) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = colors.textSecondary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.selectedDayLabel,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = report.selectedDayRange,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        IconButton(onClick = onNextDay, enabled = report.canGoNextDay) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = if (report.canGoNextDay) colors.textSecondary else colors.textMuted.copy(alpha = 0.45f),
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    HourlyChart(
        hours = report.hourlyTimeline,
    )
    Spacer(Modifier.height(12.dp))
    TimelineLegend()
    Spacer(Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.battery_intelligence_charging_periods),
        style = MaterialTheme.typography.titleMedium,
        color = colors.textPrimary,
    )
    Spacer(Modifier.height(8.dp))
    if (report.dailyChargingSessions.isEmpty()) {
        Text(
            text = stringResource(R.string.battery_intelligence_periods_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    } else {
        val visibleSessions = report.dailyChargingSessions.take(MAX_VISIBLE_CHARGING_SESSIONS)
        visibleSessions.forEachIndexed { index, session ->
            ChargingSessionCard(
                session = session,
                onClick = { onChargingSessionClick(session) },
            )
            if (index != visibleSessions.lastIndex) Spacer(Modifier.height(8.dp))
        }
        if (report.dailyChargingSessions.size > MAX_VISIBLE_CHARGING_SESSIONS) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { onShowAllChargingSessionsClick(report.selectedDayStartMillis) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.battery_intelligence_show_all_periods,
                        report.dailyChargingSessions.size,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ChargingSessionCard(
    session: ChargingSessionSummary,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, if (session.needsAdvice) colors.warning.copy(alpha = 0.35f) else colors.success.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${session.averageWatts.formatWatts()} avg • ${session.maxTemperatureCelsius}°C max",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
        StatusPill(
            status = if (session.needsAdvice) MetricStatus.Warning else MetricStatus.Normal,
            label = if (session.needsAdvice) {
                stringResource(R.string.battery_intelligence_session_needs_advice)
            } else {
                stringResource(R.string.battery_intelligence_session_good)
            },
        )
    }
}

private fun formatDelta(value: Int): String =
    if (value >= 0) "+$value%" else "$value%"

@Composable
private fun HourlyChart(
    hours: List<ChargingHourSlot>,
) {
    val colors = AppTheme.colors
    val gridColor = colors.border.copy(alpha = 0.75f)
    val dashedGridColor = colors.textMuted.copy(alpha = 0.28f)
    val labelColor = colors.textMuted
    val goodColor = ChargingHourStatus.GoodCharging.chartColor()
    val poorColor = ChargingHourStatus.PoorCharging.chartColor()
    val dischargeColor = ChargingHourStatus.Discharging.chartColor()
    val stableColor = ChargingHourStatus.Stable.chartColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1D1D1F))
            .border(1.dp, colors.border.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val hourWidth = chartWidth / 24f
                val barWidth = hourWidth * 0.68f
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                    val y = chartHeight * fraction
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1f,
                    )
                }
                listOf(0, 6, 12, 18, 24).forEach { hour ->
                    val x = hour * hourWidth
                    drawLine(
                        color = dashedGridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, chartHeight),
                        strokeWidth = 1f,
                        pathEffect = dash,
                    )
                }

                fun colorFor(status: ChargingHourStatus): Color = when (status) {
                    ChargingHourStatus.GoodCharging -> goodColor
                    ChargingHourStatus.PoorCharging -> poorColor
                    ChargingHourStatus.Discharging -> dischargeColor
                    ChargingHourStatus.Stable -> stableColor
                    ChargingHourStatus.NoData -> Color.Transparent
                }

                hours.forEach { slot ->
                    val left = slot.hour * hourWidth + (hourWidth - barWidth) / 2f
                    slot.segments.forEach segmentLoop@{ segment ->
                        if (segment.durationMinutes <= 0f) return@segmentLoop
                        val segmentStart = segment.startMinute.coerceIn(0f, 60f)
                        val segmentEnd = (segment.startMinute + segment.durationMinutes).coerceIn(0f, 60f)
                        val bottom = chartHeight - (segmentStart / 60f) * chartHeight
                        val top = chartHeight - (segmentEnd / 60f) * chartHeight
                        drawRoundRect(
                            color = colorFor(segment.status),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, bottom - top),
                            cornerRadius = CornerRadius(4f, 4f),
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .height(160.dp)
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text("60 хв", style = MaterialTheme.typography.labelLarge, color = labelColor.copy(alpha = 0.62f))
                Text("30 хв", style = MaterialTheme.typography.labelLarge, color = labelColor.copy(alpha = 0.62f))
                Text("0", style = MaterialTheme.typography.labelLarge, color = labelColor.copy(alpha = 0.62f))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 58.dp, top = 8.dp),
        ) {
            listOf("00", "06", "12", "18").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = labelColor.copy(alpha = 0.62f),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TimelineLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LegendItem(stringResource(R.string.battery_intelligence_legend_good), ChargingHourStatus.GoodCharging.chartColor(), Modifier.weight(1f))
        LegendItem(stringResource(R.string.battery_intelligence_legend_poor), ChargingHourStatus.PoorCharging.chartColor(), Modifier.weight(1f))
        LegendItem(stringResource(R.string.battery_intelligence_legend_discharge), ChargingHourStatus.Discharging.chartColor(), Modifier.weight(1f))
        LegendItem(stringResource(R.string.battery_intelligence_legend_stable), ChargingHourStatus.Stable.chartColor(), Modifier.weight(1f))
    }
}

@Composable
private fun LegendItem(label: String, color: Color, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
    }
}

@Composable
private fun ChargingHistoryRow(entry: ChargingHistoryEntry) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.label, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${entry.status} • ${entry.temperatureCelsius}°C",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${entry.levelPercent}%", style = MaterialTheme.typography.titleMedium, color = colors.batteryColor)
            Text(entry.watts.formatWatts(), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
        }
    }
}

@Composable
private fun PremiumLockedBatteryIntelligence(
    onSubscribeClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val feedback = LocalAppFeedback.current

    AccentCard(accentColor = colors.batteryColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StatusPill(MetricStatus.PermissionRequired, stringResource(R.string.subscription_status_premium))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.battery_intelligence_locked_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.battery_intelligence_locked_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f))
                    .border(1.dp, colors.accent.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                feedback?.confirm()
                onSubscribeClick()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent,
                contentColor = colors.background,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.battery_intelligence_unlock_button))
        }
    }
}

@Composable
private fun BatteryIntelligenceHero(report: BatteryIntelligenceReport) {
    val colors = AppTheme.colors
    AccentCard(accentColor = colors.batteryColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StatusPill(MetricStatus.Normal, stringResource(R.string.subscription_status_premium))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.battery_intelligence_title),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = report.degradationSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(colors.batteryColor.copy(alpha = 0.12f))
                    .border(1.dp, colors.batteryColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.BatteryChargingFull,
                        contentDescription = null,
                        tint = colors.batteryColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "${estimateCapacityRetentionPercent(report.healthScore)}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, title: String) {
    val colors = AppTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceHover),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(19.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
    }
}

@Composable
private fun AdviceRow(number: Int, text: String) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(colors.batteryColor.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.batteryColor,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Float?.formatWatts(): String =
    this?.let { "%.2f W".format(it) } ?: stringResource(R.string.common_not_reported)

@Composable
private fun ChargingHourStatus.chartColor(): Color {
    val colors = AppTheme.colors
    return when (this) {
        ChargingHourStatus.GoodCharging -> colors.success
        ChargingHourStatus.PoorCharging -> colors.warning
        ChargingHourStatus.Discharging -> colors.critical
        ChargingHourStatus.Stable -> colors.textMuted.copy(alpha = 0.7f)
        ChargingHourStatus.NoData -> colors.textMuted
    }
}

private const val MAX_VISIBLE_CHARGING_SESSIONS = 4
