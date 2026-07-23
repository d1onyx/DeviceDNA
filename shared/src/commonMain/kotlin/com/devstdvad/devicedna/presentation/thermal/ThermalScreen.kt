package com.devstdvad.devicedna.presentation.thermal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.ThermalTile
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.domain.model.ThermalZoneType
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.presentation.common.SettingsFormatters
import com.devstdvad.devicedna.di.resolveViewModel
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun ThermalScreen(
    viewModel: ThermalViewModel = resolveViewModel(ThermalViewModel::class),
    settings: UserSettings = UserSettings(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading && state.info == null) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: stringRes("thermal_load_error")); return }

    // Sort: hottest zones first, unknown last
    val sorted = info.zones.sortedWith(
        compareByDescending<com.devstdvad.devicedna.domain.model.ThermalZone> { it.temperatureCelsius ?: -1f }
            .thenBy { it.type == ThermalZoneType.Unknown },
    )

    val maxTemp = sorted.mapNotNull { it.temperatureCelsius }.maxOrNull()
    val hotZones = sorted.count { (it.temperatureCelsius ?: 0f) >= 42f }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.Bottom,
            ) {
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = if (maxTemp == null) stringRes("thermal_title_system") else stringRes("thermal_title_zones", sorted.size),
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.textPrimary,
                    )
                    if (hotZones > 0) {
                        Text(
                            text = stringRes("thermal_zones_hot", hotZones),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.thermalColor,
                        )
                    } else if (maxTemp == null) {
                        Text(
                            text = stringRes("thermal_coarse_state_note"),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }
                if (maxTemp != null) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(
                            text = SettingsFormatters.formatTemperatureWhole(maxTemp, settings.temperatureUnit),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (maxTemp >= 60f) colors.critical else if (maxTemp >= 42f) colors.thermalColor else colors.sensorsColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(stringRes("thermal_field_peak"), style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                    }
                } else {
                    val stateName = sorted.firstOrNull()?.name
                    if (stateName != null) {
                        androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text(
                                text = stateName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = when (stateName.lowercase()) {
                                    "critical" -> colors.critical
                                    "serious" -> colors.thermalColor
                                    "fair" -> colors.warning
                                    else -> colors.sensorsColor
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(stringRes("thermal_field_state"), style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        }
                    }
                }
            }
        }

        items(sorted) { zone ->
            val hasTemp = zone.temperatureCelsius != null
            ThermalTile(
                label = if (hasTemp) zone.name else stringRes("thermal_tile_system_label"),
                tempCelsius = zone.temperatureCelsius,
                temperatureText = zone.temperatureCelsius?.let {
                    SettingsFormatters.formatTemperature(it, settings.temperatureUnit)
                },
                stateName = if (hasTemp) null else zone.name,
            )
        }
    }
}
