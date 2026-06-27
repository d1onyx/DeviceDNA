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
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.ThermalTile
import com.devstdvad.devicedna.domain.model.ThermalZoneType
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun ThermalScreen(
    viewModel: ThermalViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading && state.info == null) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: "Thermal info unavailable"); return }

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
                        text = "${sorted.size} Thermal Zones",
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.textPrimary,
                    )
                    if (hotZones > 0) {
                        Text(
                            text = "$hotZones zone${if (hotZones != 1) "s" else ""} running hot",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.thermalColor,
                        )
                    }
                }
                maxTemp?.let {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(
                            text = "%.0f°C".format(it),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (it >= 60f) colors.critical else if (it >= 42f) colors.thermalColor else colors.sensorsColor,
                        )
                        Text("Peak", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                    }
                }
            }
        }

        items(sorted) { zone ->
            ThermalTile(label = zone.name, tempCelsius = zone.temperatureCelsius)
        }
    }
}
