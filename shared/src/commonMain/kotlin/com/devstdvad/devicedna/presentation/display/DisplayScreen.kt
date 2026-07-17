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
import com.devstdvad.devicedna.di.resolveViewModel
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun DisplayScreen(
    viewModel: DisplayViewModel = resolveViewModel(DisplayViewModel::class),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: stringRes("common_unavailable")); return }

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
                            text = info.physicalSizeInches?.let { stringRes("display_hero_type_size", info.displayType, Formatters.oneDecimal(it)) } ?: info.displayType,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.displayColor,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringRes("display_value_hz", info.refreshRateHz.toInt()),
                            style = MaterialTheme.typography.displayMedium,
                            color = colors.displayColor,
                        )
                        Text(stringRes("display_field_refresh_rate"), style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                    }
                }
                if (info.supportedRefreshRates.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringRes("display_supported_rates", info.supportedRefreshRates.joinToString(", ") { "${it.toInt()} Hz" }),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
            }
        }

        item {
            SectionCard {
                Text(stringRes("display_section_specs"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(stringRes("display_field_resolution"), stringRes("display_value_resolution_px", info.widthPx, info.heightPx), copyable = false)
                InfoRow(stringRes("display_field_density"), info.densityDpi?.let { stringRes("display_value_density", it, info.densityBucket) } ?: stringRes("common_unavailable"), copyable = false)
                InfoRow(stringRes("display_field_physical_size"), info.physicalSizeInches?.let { stringRes("display_value_inches", Formatters.oneDecimal(it)) } ?: stringRes("common_unavailable"), copyable = false)
                InfoRow(stringRes("network_field_type"), info.displayType, copyable = false)
                InfoRow(stringRes("display_field_orientation"), info.orientation, copyable = false)
                InfoRow(stringRes("display_field_wide_color_gamut"), yesNoUnknown(info.isWideColorGamut), copyable = false)
                InfoRow(stringRes("display_field_hdr"), supportUnknown(info.isHdr), copyable = false)
                InfoRow(stringRes("display_field_font_scale"), info.fontScale?.let { stringRes("display_value_multiplier", Formatters.twoDecimals(it)) } ?: stringRes("common_unavailable"), copyable = false, showDivider = false)
            }
        }

        item {
            AccentCard(accentColor = colors.displayColor) {
                Text(stringRes("display_section_brightness"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringRes("display_field_level"), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    Text("${(info.brightnessLevel * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                }
                Spacer(Modifier.height(6.dp))
                LiveBar(fraction = info.brightnessLevel, accentColor = colors.displayColor)
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = stringRes("display_field_adaptive_brightness"),
                    value = info.isAdaptiveBrightness?.let { if (it) stringRes("common_enabled") else stringRes("common_disabled") } ?: stringRes("common_unavailable"),
                    copyable = false,
                    showDivider = false,
                )
            }
        }
    }
}

@Composable
private fun yesNoUnknown(value: Boolean?): String = value?.let {
    if (it) stringRes("common_yes") else stringRes("common_no")
} ?: stringRes("common_unavailable")

@Composable
private fun supportUnknown(value: Boolean?): String = value?.let {
    if (it) stringRes("connectivity_value_supported") else stringRes("connectivity_value_not_supported")
} ?: stringRes("common_unavailable")
