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
import com.devstdvad.devicedna.core.common.Formatters
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
import com.devstdvad.devicedna.di.resolveViewModel
import com.devstdvad.devicedna.platform.PlatformInfo
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun BatteryScreen(
    viewModel: BatteryViewModel = resolveViewModel(BatteryViewModel::class),
    settings: UserSettings = UserSettings(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors
    val isIos = PlatformInfo.isIos

    if (state.isLoading && state.info == null) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: stringRes("battery_load_error")); return }

    val isCharging = info.status == BatteryStatus.Charging || info.status == BatteryStatus.Full
    val batteryStatus = when {
        !isIos && info.temperatureCelsius >= 45f -> MetricStatus.Critical
        !isIos && info.health != BatteryHealth.Good -> MetricStatus.Warning
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
                            if (isIos) {
                                StatChip(stringRes("battery_field_source"), info.source.name)
                                StatChip(stringRes("battery_field_power_saver"), if (info.isPowerSaveMode) stringRes("common_on") else stringRes("common_off"))
                            } else {
                                StatChip(stringRes("battery_field_temp_short"), SettingsFormatters.formatTemperature(info.temperatureCelsius, settings.temperatureUnit))
                                StatChip(stringRes("battery_field_voltage"), stringRes("battery_value_mv", info.voltageMv))
                                info.currentMa?.let { StatChip(stringRes("battery_field_current"), stringRes("battery_value_ma", it)) }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        info.estimatedWatts?.let {
                            Text(stringRes("battery_value_watts", Formatters.twoDecimals(it)), style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
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
                Text(stringRes("battery_section_status"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(stringRes("battery_field_charging_status"), info.status.name, copyable = false)
                InfoRow(stringRes("battery_field_power_source"), info.source.name, copyable = false)
                InfoRow(stringRes("battery_field_health"), info.health.name, copyable = false)
                InfoRow(stringRes("battery_field_power_saver"), if (info.isPowerSaveMode) stringRes("common_enabled") else stringRes("common_disabled"), copyable = false)
                InfoRow(stringRes("battery_field_technology"), info.technology, copyable = false, showDivider = false)
            }
        }

        // Measurements
        item {
            AccentCard(accentColor = batteryAccent) {
                Text(stringRes("battery_section_measurements"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                if (isIos) {
                    // iOS never exposes battery temperature, voltage, capacity or cycle count to apps.
                    InfoRow(stringRes("battery_field_temperature"), stringRes("common_unavailable_ios"), copyable = false)
                    InfoRow(stringRes("battery_field_voltage"), stringRes("common_unavailable_ios"), copyable = false)
                    InfoRow(stringRes("battery_field_capacity"), stringRes("common_unavailable_ios"), copyable = false)
                    InfoRow(
                        label = stringRes("battery_field_charge_cycles"),
                        value = stringRes("common_unavailable_ios"),
                        copyable = false,
                        showDivider = false,
                    )
                } else {
                    InfoRow(stringRes("battery_field_temperature"), SettingsFormatters.formatTemperature(info.temperatureCelsius, settings.temperatureUnit), copyable = false)
                    InfoRow(stringRes("battery_field_voltage"), stringRes("battery_value_mv", info.voltageMv), copyable = false)
                    info.currentMa?.let { InfoRow(stringRes("battery_field_current"), stringRes("battery_value_ma", it), copyable = false) }
                    info.estimatedWatts?.let { InfoRow(stringRes("battery_field_power"), stringRes("battery_value_watts", Formatters.twoDecimals(it)), copyable = false) }
                    InfoRow(stringRes("battery_field_charge_time_remaining"), formatChargeTime(info.chargeTimeRemainingMs), copyable = false)
                    info.capacityMah?.let { InfoRow(stringRes("battery_field_current_capacity"), stringRes("battery_value_mah", it), copyable = false) }
                    InfoRow(
                        label = stringRes("battery_field_charge_cycles"),
                        value = info.chargeCycles?.toString() ?: stringRes("battery_value_na_android14"),
                        copyable = false,
                        showDivider = false,
                    )
                }
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

@Composable
private fun formatChargeTime(millis: Long?): String {
    if (millis == null || millis <= 0L) return stringRes("common_not_reported")
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60L; val minutes = totalMinutes % 60L
    return if (hours > 0L) stringRes("battery_charge_time_hm", hours, minutes) else stringRes("battery_charge_time_m", minutes)
}
