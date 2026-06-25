package com.devstdvad.devicedna.presentation.overview

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.ErrorBanner
import com.devstdvad.devicedna.core.design.component.GaugeRing
import com.devstdvad.devicedna.core.design.component.HealthScoreRing
import com.devstdvad.devicedna.core.design.component.InsightCard
import com.devstdvad.devicedna.core.design.component.LiveBar
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.domain.model.ConnectionType
import com.devstdvad.devicedna.domain.model.InsightSeverity
import com.devstdvad.devicedna.domain.model.ThermalZoneType
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel = koinViewModel(),
    onSettingsClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors
    var errorDismissed by remember { mutableStateOf(false) }

    if (state.isLoading && state.storage == null && state.healthScore == null) {
        LoadingScreen()
        return
    }

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
                        text = state.deviceModel ?: "DeviceDNA",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Device Dashboard",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.refresh(); errorDismissed = false }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = colors.textSecondary)
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = colors.textSecondary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Error banner (always present; AnimatedVisibility handles show/hide)
            item {
                ErrorBanner(
                    message = if (state.error != null && !errorDismissed) state.error else null,
                    onDismiss = { errorDismissed = true; viewModel.dismissError() },
                )
            }

            // Live gauges row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val cpuUsage = state.cpuUsage ?: 0f
                    GaugeRing(
                        value = cpuUsage,
                        label = "CPU",
                        valueText = "${cpuUsage.toInt()}%",
                        accentColor = colors.cpuColor,
                        size = 100.dp,
                        strokeWidth = 9.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                    )

                    val ram = state.ram
                    val ramPct = ((ram?.usedPercent ?: 0f) * 100f).toInt()
                    GaugeRing(
                        value = ramPct.toFloat(),
                        label = "RAM",
                        valueText = "$ramPct%",
                        subLabel = ram?.let { Formatters.formatBytesShort(it.usedBytes) },
                        accentColor = colors.ramColor,
                        size = 100.dp,
                        strokeWidth = 9.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                    )

                    val bat = state.battery
                    val batColor = when {
                        bat == null -> colors.batteryColor
                        bat.temperatureCelsius >= 45f -> colors.critical
                        bat.levelPercent <= 15 -> colors.warning
                        else -> colors.batteryColor
                    }
                    GaugeRing(
                        value = (bat?.levelPercent ?: 0).toFloat(),
                        label = "Battery",
                        valueText = "${bat?.levelPercent ?: 0}%",
                        subLabel = bat?.status?.name?.take(5),
                        accentColor = batColor,
                        size = 100.dp,
                        strokeWidth = 9.dp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp),
                    )
                }
            }

            // Health score card
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Health Score",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.textMuted,
                            )
                            Spacer(Modifier.height(3.dp))
                            val healthScore = state.healthScore
                            Text(
                                text = if (healthScore != null) {
                                    when {
                                        healthScore.overall >= 80 -> "Excellent"
                                        healthScore.overall >= 60 -> "Good"
                                        healthScore.overall >= 40 -> "Fair"
                                        else -> "Needs Attention"
                                    }
                                } else "Analyzing…",
                                style = MaterialTheme.typography.displaySmall,
                                color = colors.textPrimary,
                            )
                            state.healthScore?.let { score ->
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    ScorePill("Battery", score.battery)
                                    ScorePill("Thermal", score.thermal)
                                    ScorePill("Security", score.security)
                                    ScorePill("Storage", score.storage)
                                }
                            }
                        }
                        HealthScoreRing(
                            score = state.healthScore?.overall ?: 0,
                            size = 86.dp,
                            strokeWidth = 9.dp,
                        )
                    }
                }
            }

            // Health insights (top critical/warning only)
            val topInsights = state.healthScore?.insights
                ?.filter { it.severity == InsightSeverity.Critical || it.severity == InsightSeverity.Warning }
                ?.sortedByDescending { it.severity.ordinal }
                ?.take(3)
            if (!topInsights.isNullOrEmpty()) {
                item {
                    SectionCard {
                        Text(
                            "Insights",
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            topInsights.forEach { insight ->
                                InsightCard(insight = insight)
                            }
                        }
                    }
                }
            }

            // Network quick card
            state.network?.let { net ->
                item {
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                val netIcon = when (net.connectionType) {
                                    ConnectionType.WiFi -> Icons.Outlined.Wifi
                                    ConnectionType.Cellular -> Icons.Outlined.SignalCellularAlt
                                    ConnectionType.None -> Icons.Outlined.WifiOff
                                    else -> Icons.Outlined.Wifi
                                }
                                val netColor = if (net.connectionType == ConnectionType.None) colors.textMuted else colors.networkColor
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(netColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(netIcon, null, tint = netColor, modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    val typeLabel = when (net.connectionType) {
                                        ConnectionType.WiFi -> net.ssid ?: "Wi-Fi"
                                        ConnectionType.Cellular -> net.cellularGeneration ?: "Cellular"
                                        ConnectionType.None -> "Offline"
                                        else -> net.connectionType.name
                                    }
                                    Text(typeLabel, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    val subLabel = when {
                                        net.connectionType == ConnectionType.None -> "No internet"
                                        net.isVpnActive -> "VPN active"
                                        net.linkSpeedMbps != null -> "${net.linkSpeedMbps} Mbps"
                                        net.isValidatedInternet -> "Connected"
                                        else -> "Limited connectivity"
                                    }
                                    Text(subLabel, style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
                                }
                            }
                            if (net.connectionType != ConnectionType.None) {
                                net.signalStrength?.let { rssi ->
                                    val bars = when {
                                        rssi >= -50 -> 4
                                        rssi >= -60 -> 3
                                        rssi >= -70 -> 2
                                        rssi >= -80 -> 1
                                        else -> 0
                                    }
                                    SignalBars(bars = bars, color = colors.networkColor)
                                }
                            }
                        }
                    }
                }
            }

            // Storage bar card
            state.storage?.let { storage ->
                item {
                    SectionCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(colors.storageColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Outlined.Storage, null, tint = colors.storageColor, modifier = Modifier.size(16.dp))
                                }
                                Column {
                                    Text("Storage", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                    Text(
                                        "${Formatters.formatBytes(storage.usedBytes)} used of ${Formatters.formatBytes(storage.totalBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                    )
                                }
                            }
                            val storageAccent = when {
                                storage.usedPercent >= 0.95f -> colors.critical
                                storage.usedPercent >= 0.85f -> colors.warning
                                else -> colors.storageColor
                            }
                            Text(
                                text = "${(storage.usedPercent * 100).toInt()}%",
                                style = MaterialTheme.typography.headlineSmall,
                                color = storageAccent,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        val storageAccent = when {
                            storage.usedPercent >= 0.95f -> colors.critical
                            storage.usedPercent >= 0.85f -> colors.warning
                            else -> colors.storageColor
                        }
                        LiveBar(fraction = storage.usedPercent, accentColor = storageAccent, height = 6.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${Formatters.formatBytes(storage.freeBytes)} free",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                        )
                    }
                }
            }

            // Thermal quick card
            state.thermal?.let { thermal ->
                val cpuZone = thermal.zones.firstOrNull { it.type == ThermalZoneType.Cpu }
                    ?: thermal.zones.firstOrNull()
                cpuZone?.temperatureCelsius?.let { temp ->
                    item {
                        val thermalAccent = when {
                            temp >= 60f -> colors.critical
                            temp >= 50f -> colors.warning
                            else -> colors.thermalColor
                        }
                        SectionCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(thermalAccent.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Outlined.Thermostat, null, tint = thermalAccent, modifier = Modifier.size(16.dp))
                                    }
                                    Column {
                                        Text("Thermal", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                        Text(
                                            "${thermal.zones.size} zone${if (thermal.zones.size != 1) "s" else ""} monitored",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textMuted,
                                        )
                                    }
                                }
                                Text(
                                    "%.1f°C".format(temp),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = thermalAccent,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(contentPadding.calculateBottomPadding())) }
        }
    }
}

@Composable
private fun ScorePill(label: String, value: Int) {
    val colors = AppTheme.colors
    val color = when {
        value >= 80 -> colors.success
        value >= 60 -> colors.warning
        else -> colors.critical
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
    }
}

@Composable
private fun SignalBars(bars: Int, color: androidx.compose.ui.graphics.Color) {
    val colors = AppTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        (1..4).forEach { i ->
            Box(
                modifier = Modifier
                    .size(width = 5.dp, height = (4 + i * 4).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= bars) color else colors.border),
            )
        }
    }
}
