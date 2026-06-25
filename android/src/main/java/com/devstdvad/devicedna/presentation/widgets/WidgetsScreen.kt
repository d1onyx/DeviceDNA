package com.devstdvad.devicedna.presentation.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Battery5Bar
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.MetricCard
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.presentation.overview.OverviewViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun WidgetsScreen(viewModel: OverviewViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard {
                Text("Widget Previews", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Privacy", "Sensitive values hidden by default")
                InfoRow("Refresh", "Uses platform-safe intervals")
                InfoRow("Tap action", "Opens the related section", showDivider = false)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "RAM",
                    primaryValue = "${((state.ram?.usedPercent ?: 0f) * 100).toInt()}%",
                    secondaryValue = "Compact",
                    icon = Icons.Outlined.Memory,
                    progress = state.ram?.usedPercent ?: 0f,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Battery",
                    primaryValue = "${state.battery?.levelPercent ?: 0}%",
                    secondaryValue = state.battery?.status?.name ?: "Loading",
                    icon = Icons.Outlined.Battery5Bar,
                    progress = (state.battery?.levelPercent ?: 0) / 100f,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "Storage",
                    primaryValue = "${((state.storage?.usedPercent ?: 0f) * 100).toInt()}%",
                    secondaryValue = "Internal",
                    icon = Icons.Outlined.Storage,
                    progress = state.storage?.usedPercent ?: 0f,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Thermal",
                    primaryValue = "${state.battery?.temperatureCelsius ?: 0f}°C",
                    secondaryValue = "Battery",
                    icon = Icons.Outlined.Thermostat,
                    status = if ((state.battery?.temperatureCelsius ?: 0f) >= 45f) MetricStatus.Warning else MetricStatus.Normal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
