package com.devstdvad.devicedna.presentation.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.PrivacyMask
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.AccentCard
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.data.settings.DataUnit
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.domain.model.ConnectionType
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.presentation.common.SettingsFormatters
import com.devstdvad.devicedna.di.resolveViewModel
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel = resolveViewModel(NetworkViewModel::class),
    settings: UserSettings = UserSettings(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    LaunchedEffect(settings.publicIpEnabled) {
        viewModel.setPublicIpLookupEnabled(settings.publicIpEnabled)
    }

    if (state.isLoading) { LoadingScreen(); return }

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
        state.network?.let { net ->
            item {
                val isWifi = net.connectionType == ConnectionType.WiFi
                val isCellular = net.connectionType == ConnectionType.Cellular
                val icon = if (isWifi) Icons.Outlined.Wifi else Icons.Outlined.SignalCellularAlt
                val typeLabel = when (net.connectionType) {
                    ConnectionType.WiFi -> net.ssid?.let {
                        if (settings.maskSensitive) PrivacyMask.maskSsid(it) else it
                    } ?: stringRes("network_type_wifi")
                    ConnectionType.Cellular -> net.cellularGeneration ?: stringRes("network_type_cellular")
                    ConnectionType.Ethernet -> stringRes("network_type_ethernet")
                    ConnectionType.None -> stringRes("network_value_offline")
                    ConnectionType.Unknown -> stringRes("common_unknown")
                }

                AccentCard(accentColor = colors.networkColor) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                    .background(colors.networkColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(icon, null, tint = colors.networkColor, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(typeLabel, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                                if (net.connectionType != ConnectionType.None) {
                                    Text(
                                        text = if (net.isMetered) stringRes("network_value_metered") else stringRes("network_value_unmetered"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (net.isMetered) colors.warning else colors.networkColor,
                                    )
                                }
                                val cellularOperator = net.cellularOperator
                                if (isCellular && cellularOperator != null) {
                                    Text(cellularOperator, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                                }
                            }
                        }
                        // Signal strength indicator
                        net.signalStrength?.let { rssi ->
                            val bars = rssiToBars(rssi)
                            SignalBars(bars = bars, color = colors.networkColor)
                        }
                    }

                    // Speed display
                    if (net.rxBytesPerSec != null || net.txBytesPerSec != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            net.rxBytesPerSec?.let { rx ->
                                SpeedChip(
                                    icon = Icons.Outlined.ArrowDownward,
                                    speed = rx,
                                    label = stringRes("network_label_download"),
                                    dataUnit = settings.dataUnit,
                                    color = colors.success,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            net.txBytesPerSec?.let { tx ->
                                SpeedChip(
                                    icon = Icons.Outlined.ArrowUpward,
                                    speed = tx,
                                    label = stringRes("network_label_upload"),
                                    dataUnit = settings.dataUnit,
                                    color = colors.networkColor,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Text(stringRes("network_section_connection"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow(stringRes("network_field_type"), net.connectionType.name, copyable = false)
                    net.wifiStandard?.let { InfoRow(stringRes("network_field_wifi_standard"), it, copyable = false) }
                    net.linkSpeedMbps?.let { InfoRow(stringRes("network_field_link_speed"), stringRes("network_value_mbps", it), copyable = false) }
                    net.frequencyMhz?.let { InfoRow(stringRes("network_field_frequency"), stringRes("network_value_mhz", it), copyable = false) }
                    net.channel?.let { InfoRow(stringRes("network_field_channel"), it.toString(), copyable = false) }
                    net.interfaceName?.let { InfoRow(stringRes("network_field_interface"), it, copyable = false) }
                    if (net.activeTransports.isNotEmpty()) {
                        InfoRow(stringRes("network_field_transports"), net.activeTransports.joinToString(", "), copyable = false)
                    }
                    net.macAddress?.let {
                        InfoRow(
                            stringRes("network_field_mac"),
                            it,
                            maskedValue = if (settings.maskSensitive) PrivacyMask.maskMac(it) else null,
                            copyable = true,
                            showDivider = false,
                        )
                    }
                        ?: InfoRow(stringRes("network_field_status"), if (net.connectionType == ConnectionType.None) stringRes("network_value_offline") else stringRes("network_value_connected"), copyable = false, showDivider = false)
                }
            }

            item {
                SectionCard {
                    Text(stringRes("network_section_risk"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow(stringRes("network_field_vpn"), if (net.isVpnActive) stringRes("network_value_active") else stringRes("network_value_not_active"), copyable = false)
                    InfoRow(stringRes("network_field_validated_internet"), net.isValidatedInternet?.let { if (it) stringRes("common_yes") else stringRes("common_no") } ?: stringRes("common_unknown"), copyable = false)
                    InfoRow(stringRes("network_field_captive_portal"), net.isCaptivePortal?.let { if (it) stringRes("common_detected") else stringRes("common_no") } ?: stringRes("common_unknown"), copyable = false)
                    InfoRow(stringRes("network_field_private_dns"), net.privateDnsServerName ?: stringRes("common_not_reported"), copyable = false)
                    InfoRow(
                        label = stringRes("network_field_http_proxy"),
                        value = net.httpProxyHost?.let { "$it:${net.httpProxyPort ?: 0}" } ?: stringRes("common_not_configured"),
                        copyable = false,
                        showDivider = false,
                    )
                }
            }

            item {
                SectionCard {
                    Text(stringRes("network_section_addresses"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    net.localIpv4?.let {
                        InfoRow(stringRes("network_field_ipv4"), it, maskedValue = if (settings.maskSensitive) PrivacyMask.maskIpv4(it) else null)
                    }
                        ?: InfoRow(stringRes("network_field_ipv4"), stringRes("common_not_available"), copyable = false)
                    net.localIpv6?.let {
                        InfoRow(stringRes("network_field_ipv6"), it, maskedValue = if (settings.maskSensitive) PrivacyMask.maskIpv6(it) else null)
                    }
                        ?: InfoRow(stringRes("network_field_ipv6"), stringRes("common_not_available"), copyable = false)
                    net.gateway?.let {
                        InfoRow(stringRes("network_field_gateway"), it, maskedValue = if (settings.maskSensitive) PrivacyMask.maskIpv4(it) else null)
                    }
                    net.subnetMask?.let { InfoRow(stringRes("network_field_subnet"), it, copyable = false) }
                    InfoRow(
                        label = stringRes("network_field_public_ip"),
                        value = when {
                            !settings.publicIpEnabled -> stringRes("network_value_disabled_privacy")
                            state.publicIpLoading -> stringRes("common_loading")
                            state.publicIp != null -> state.publicIp ?: stringRes("common_not_available")
                            else -> state.publicIpError ?: stringRes("network_public_ip_lookup_failed")
                        },
                        maskedValue = state.publicIp
                            ?.takeIf { settings.publicIpEnabled && settings.maskSensitive }
                            ?.let(::maskIp),
                        copyable = settings.publicIpEnabled && state.publicIp != null,
                        showDivider = net.dns.isNotEmpty(),
                    )
                    if (net.dns.isNotEmpty()) InfoRow(stringRes("network_field_dns"), net.dns.joinToString(", "), copyable = true, showDivider = false)
                }
            }
        }
    }
}

@Composable
private fun SpeedChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    speed: Long,
    label: String,
    dataUnit: DataUnit,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Column {
            Text(formatSpeed(speed, dataUnit), style = MaterialTheme.typography.titleSmall, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
        }
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

private fun rssiToBars(rssi: Int): Int = when {
    rssi >= -50 -> 4
    rssi >= -60 -> 3
    rssi >= -70 -> 2
    rssi >= -80 -> 1
    else -> 0
}

private fun formatSpeed(bytesPerSec: Long, dataUnit: DataUnit): String {
    return SettingsFormatters.formatBytesPerSecond(bytesPerSec, dataUnit)
}

private fun maskIp(ip: String): String =
    if (":" in ip) PrivacyMask.maskIpv6(ip) else PrivacyMask.maskIpv4(ip)
