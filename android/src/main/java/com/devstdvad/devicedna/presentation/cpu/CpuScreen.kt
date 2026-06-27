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
import org.koin.androidx.compose.koinViewModel

@Composable
fun CpuScreen(
    viewModel: CpuViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading && state.info == null) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: "Could not load CPU info"); return }

    val maxFreqKhz = info.cores.maxOfOrNull { it.maxFrequencyKhz }?.toInt() ?: 1
    val usagePercent = info.usagePercent ?: 0f
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
                            SpecChip("Cores", "${info.coreCount}")
                            avgFreqMhz?.let { SpecChip("Avg Freq", "${it} MHz") }
                            SpecChip("Governor", info.governor)
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
                        value = usagePercent,
                        label = "CPU Load",
                        valueText = "${usagePercent.toInt()}%",
                        accentColor = colors.cpuColor,
                        size = 90.dp,
                        strokeWidth = 9.dp,
                    )
                }
                info.processCount?.let { count ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "$count running processes",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
            }
        }

        // CPU Cores live grid
        item {
            SectionCard {
                Text("CPU Cores · Live", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
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

        // Clusters
        if (info.clusters.isNotEmpty()) {
            item {
                AccentCard(accentColor = colors.cpuColor) {
                    Text("Architecture", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    info.clusters.forEachIndexed { i, cluster ->
                        InfoRow(
                            label = cluster.name,
                            value = "Cores ${cluster.coreIndices.joinToString(",")} · ${cluster.minFrequencyMhz}–${cluster.maxFrequencyMhz} MHz",
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
                Text("GPU", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Renderer", info.gpu.renderer, copyable = false)
                InfoRow("Vendor", info.gpu.vendor, copyable = false)
                InfoRow("API", info.gpu.version, copyable = false, showDivider = false)
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
