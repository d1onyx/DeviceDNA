package com.devstdvad.devicedna.presentation.cpu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.AccentCard
import com.devstdvad.devicedna.core.design.component.CpuCoreTile
import com.devstdvad.devicedna.core.design.component.GaugeRing
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.di.resolveViewModel
import com.devstdvad.devicedna.platform.PlatformInfo
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun CpuScreen(
    viewModel: CpuViewModel = resolveViewModel(CpuViewModel::class),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors
    val isIos = PlatformInfo.isIos

    if (state.isLoading && state.info == null) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: stringRes("cpu_load_error")); return }

    val maxFreqKhz = info.cores.maxOfOrNull { it.maxFrequencyKhz }?.toInt() ?: 1
    val usagePercent = info.usagePercent
    val avgFreqMhz = info.cores
        .mapNotNull { if (it.isOnline) it.currentFrequencyKhz else null }
        .average().takeIf { it.isFinite() }?.toInt()?.div(1000)

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
        // Hero: usage gauge + chipset
        item {
            AccentCard(accentColor = colors.cpuColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text(info.chipsetName, style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
                        Spacer(Modifier.height(3.dp))
                        Text(info.architecture, style = MaterialTheme.typography.bodyMedium, color = colors.cpuColor)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            SpecChip(stringRes("cpu_field_cores"), "${info.coreCount}")
                            avgFreqMhz?.let { SpecChip(stringRes("cpu_field_avg_freq"), stringRes("network_value_mhz", it)) }
                            if (!isIos && info.governor.isNotBlank()) SpecChip(stringRes("cpu_field_governor"), info.governor)
                        }
                        if (info.instructionSets.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = info.instructionSets.joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textMuted,
                            )
                        }
                    }
                    GaugeRing(
                        value = usagePercent ?: 0f,
                        label = stringRes("cpu_gauge_load"),
                        valueText = usagePercent?.let { "${it.toInt()}%" } ?: "—",
                        accentColor = colors.cpuColor,
                        size = 90.dp,
                        strokeWidth = 9.dp,
                    )
                }
                info.processCount?.let { count ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringRes("cpu_running_processes", count),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
            }
        }

        // CPU Cores live grid
        item {
            SectionCard {
                Text(stringRes("cpu_section_cores_live"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                if (info.cores.isEmpty()) {
                    // iOS does not expose per-core frequency/online state to sandboxed apps.
                    Text(
                        text = if (isIos) stringRes("common_unavailable_ios") else stringRes("cpu_no_per_core_data"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                } else {
                    val rows = (info.cores.size + 1) / 2
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth().height((rows * 78).dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = false,
                    ) {
                        items(info.cores) { core ->
                            CpuCoreTile(
                                coreIndex = core.index,
                                frequencyMhz = core.currentFrequencyKhz?.div(1000)?.toInt(),
                                maxFrequencyMhz = maxFreqKhz / 1000,
                                isOnline = core.isOnline,
                            )
                        }
                    }
                }
            }
        }

        // Clusters
        if (info.clusters.isNotEmpty()) {
            item {
                AccentCard(accentColor = colors.cpuColor) {
                    Text(stringRes("device_field_architecture"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    info.clusters.forEachIndexed { i, cluster ->
                        InfoRow(
                            label = cluster.name,
                            value = stringRes("cpu_cluster_detail", cluster.coreIndices.joinToString(","), cluster.minFrequencyMhz, cluster.maxFrequencyMhz),
                            copyable = false,
                            showDivider = i < info.clusters.lastIndex,
                        )
                    }
                }
            }
        }

        // GPU
        item {
            AccentCard(accentColor = colors.displayColor) {
                Text(stringRes("cpu_section_gpu"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(stringRes("cpu_field_renderer"), info.gpu.renderer, copyable = false)
                InfoRow(stringRes("cpu_field_vendor"), info.gpu.vendor, copyable = false)
                InfoRow(stringRes("cpu_field_api"), info.gpu.version, copyable = false, showDivider = false)
            }
        }
    }
}

@Composable
private fun SpecChip(label: String, value: String) {
    val colors = AppTheme.colors
    androidx.compose.foundation.layout.Column {
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
    }
}
