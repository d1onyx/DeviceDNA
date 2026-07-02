package com.devstdvad.devicedna.presentation.display

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.AccentCard
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.LiveBar
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DisplayScreen(
    viewModel: DisplayViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: "Unavailable"); return }

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
        item {
            AccentCard(accentColor = colors.displayColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${info.widthPx} × ${info.heightPx}",
                            style = MaterialTheme.typography.displaySmall,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "${info.displayType} · ${Formatters.oneDecimal(info.physicalSizeInches)}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.displayColor,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${info.refreshRateHz.toInt()} Hz",
                            style = MaterialTheme.typography.displayMedium,
                            color = colors.displayColor,
                        )
                        Text("Refresh Rate", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                    }
                }
                if (info.supportedRefreshRates.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Supported: ${info.supportedRefreshRates.joinToString(", ") { "${it.toInt()} Hz" }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
            }
        }

        item {
            SectionCard {
                Text("Specs", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Resolution", "${info.widthPx} × ${info.heightPx} px", copyable = false)
                InfoRow("Density", "${info.densityDpi} dpi (${info.densityBucket})", copyable = false)
                InfoRow("Physical Size", "${Formatters.oneDecimal(info.physicalSizeInches)} inches", copyable = false)
                InfoRow("Type", info.displayType, copyable = false)
                InfoRow("Orientation", info.orientation, copyable = false)
                InfoRow("Wide Color Gamut", if (info.isWideColorGamut) "Yes" else "No", copyable = false)
                InfoRow("HDR", if (info.isHdr) "Supported" else "Not supported", copyable = false)
                InfoRow("Font Scale", "${Formatters.twoDecimals(info.fontScale)}×", copyable = false, showDivider = false)
            }
        }

        item {
            AccentCard(accentColor = colors.displayColor) {
                Text("Brightness", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Level", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    Text("${(info.brightnessLevel * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                }
                Spacer(Modifier.height(6.dp))
                LiveBar(fraction = info.brightnessLevel, accentColor = colors.displayColor)
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = "Adaptive Brightness",
                    value = if (info.isAdaptiveBrightness) "Enabled" else "Disabled",
                    copyable = false,
                    showDivider = false,
                )
            }
        }
    }
}
