package com.devstdvad.devicedna.presentation.battery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.AccentCard
import com.devstdvad.devicedna.core.design.component.BatteryWidget
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.core.design.component.StatusPill
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.domain.model.BatteryHealth
import com.devstdvad.devicedna.domain.model.BatteryStatus
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.presentation.common.SettingsFormatters
import org.koin.androidx.compose.koinViewModel

@Composable
fun BatteryScreen(
    viewModel: BatteryViewModel = koinViewModel(),
    settings: UserSettings = UserSettings(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading && state.info == null) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: "Could not read battery"); return }

    val isCharging = info.status == BatteryStatus.Charging || info.status == BatteryStatus.Full
    val batteryStatus = when {
        info.temperatureCelsius >= 45f -> MetricStatus.Critical
        info.health != BatteryHealth.Good -> MetricStatus.Warning
        info.levelPercent <= 15 -> MetricStatus.Warning
        else -> MetricStatus.Normal
    }
    val batteryAccent = when {
        info.temperatureCelsius >= 45f -> colors.critical
        info.levelPercent <= 15 -> colors.warning
        isCharging -> colors.success
        else -> colors.batteryColor
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero: battery shape + level
        item {
            AccentCard(accentColor = batteryAccent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${info.levelPercent}%",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = batteryAccent,
                        )
                        Spacer(Modifier.height(4.dp))
                        StatusPill(batteryStatus, info.status.name)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatChip("Temp", SettingsFormatters.formatTemperature(info.temperatureCelsius, settings.temperatureUnit))
                            StatChip("Voltage", "${info.voltageMv} mV")
                            info.currentMa?.let { StatChip("Current", "$it mA") }
                        }
                        Spacer(Modifier.height(6.dp))
                        info.estimatedWatts?.let {
                            Text("%.2f W".format(it), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    BatteryWidget(
                        levelPercent = info.levelPercent,
                        isCharging = isCharging,
                        accentColor = batteryAccent,
                        width = 52.dp,
                        height = 96.dp,
                    )
                }
            }
        }

        // Status details
        item {
            SectionCard {
                Text("Status", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Charging Status", info.status.name, copyable = false)
                InfoRow("Power Source", info.source.name, copyable = false)
                InfoRow("Health", info.health.name, copyable = false)
                InfoRow("Power Saver", if (info.isPowerSaveMode) "Enabled" else "Disabled", copyable = false)
                InfoRow("Technology", info.technology, copyable = false, showDivider = false)
            }
        }

        // Measurements
        item {
            AccentCard(accentColor = batteryAccent) {
                Text("Measurements", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Temperature", SettingsFormatters.formatTemperature(info.temperatureCelsius, settings.temperatureUnit), copyable = false)
                InfoRow("Voltage", "${info.voltageMv} mV", copyable = false)
                info.currentMa?.let { InfoRow("Current", "$it mA", copyable = false) }
                info.estimatedWatts?.let { InfoRow("Power", "%.2f W".format(it), copyable = false) }
                InfoRow("Charge Time Remaining", formatChargeTime(info.chargeTimeRemainingMs), copyable = false)
                info.capacityMah?.let { InfoRow("Current Capacity", "$it mAh", copyable = false) }
                InfoRow(
                    label = "Charge Cycles",
                    value = info.chargeCycles?.toString() ?: "N/A (Android 14+)",
                    copyable = false,
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    val colors = AppTheme.colors
    Column {
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
    }
}

private fun formatChargeTime(millis: Long?): String {
    if (millis == null || millis <= 0L) return "Not reported"
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60L; val minutes = totalMinutes % 60L
    return if (hours > 0L) "${hours}h ${minutes}m" else "${minutes}m"
}
