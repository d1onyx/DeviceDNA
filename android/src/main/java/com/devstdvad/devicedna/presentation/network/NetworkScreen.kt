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
import com.devstdvad.devicedna.domain.model.ConnectionType
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun NetworkScreen(viewModel: NetworkViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.network?.let { net ->
            item {
                val isWifi = net.connectionType == ConnectionType.WiFi
                val isCellular = net.connectionType == ConnectionType.Cellular
                val icon = if (isWifi) Icons.Outlined.Wifi else Icons.Outlined.SignalCellularAlt
                val typeLabel = when (net.connectionType) {
                    ConnectionType.WiFi -> net.ssid ?: "Wi-Fi"
                    ConnectionType.Cellular -> net.cellularGeneration ?: "Cellular"
                    ConnectionType.Ethernet -> "Ethernet"
                    ConnectionType.None -> "Offline"
                    ConnectionType.Unknown -> "Unknown"
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
                                        text = if (net.isMetered) "Metered" else "Unmetered",
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
                                    label = "Download",
                                    color = colors.success,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            net.txBytesPerSec?.let { tx ->
                                SpeedChip(
                                    icon = Icons.Outlined.ArrowUpward,
                                    speed = tx,
                                    label = "Upload",
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
                    Text("Connection", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow("Type", net.connectionType.name, copyable = false)
                    net.wifiStandard?.let { InfoRow("Wi-Fi Standard", it, copyable = false) }
                    net.linkSpeedMbps?.let { InfoRow("Link Speed", "$it Mbps", copyable = false) }
                    net.frequencyMhz?.let { InfoRow("Frequency", "$it MHz", copyable = false) }
                    net.channel?.let { InfoRow("Channel", it.toString(), copyable = false) }
                    net.interfaceName?.let { InfoRow("Interface", it, copyable = false) }
                    if (net.activeTransports.isNotEmpty()) {
                        InfoRow("Transports", net.activeTransports.joinToString(", "), copyable = false)
                    }
                    net.macAddress?.let { InfoRow("MAC", it, maskedValue = PrivacyMask.maskMac(it), copyable = true, showDivider = false) }
                        ?: InfoRow("Status", if (net.connectionType == ConnectionType.None) "Offline" else "Connected", copyable = false, showDivider = false)
                }
            }

            item {
                SectionCard {
                    Text("Network Risk", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow("VPN", if (net.isVpnActive) "Active" else "Not active", copyable = false)
                    InfoRow("Validated Internet", if (net.isValidatedInternet) "Yes" else "No", copyable = false)
                    InfoRow("Captive Portal", if (net.isCaptivePortal) "Detected" else "No", copyable = false)
                    InfoRow("Private DNS", net.privateDnsServerName ?: "Not reported", copyable = false)
                    InfoRow(
                        label = "HTTP Proxy",
                        value = net.httpProxyHost?.let { "$it:${net.httpProxyPort ?: 0}" } ?: "Not configured",
                        copyable = false,
                        showDivider = false,
                    )
                }
            }

            item {
                SectionCard {
                    Text("Addresses", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    net.localIpv4?.let { InfoRow("IPv4", it, maskedValue = PrivacyMask.maskIpv4(it)) }
                        ?: InfoRow("IPv4", "Not available", copyable = false)
                    net.localIpv6?.let { InfoRow("IPv6", it, maskedValue = PrivacyMask.maskIpv6(it)) }
                        ?: InfoRow("IPv6", "Not available", copyable = false)
                    net.gateway?.let { InfoRow("Gateway", it, maskedValue = PrivacyMask.maskIpv4(it)) }
                    net.subnetMask?.let { InfoRow("Subnet", it, copyable = false) }
                    if (net.dns.isNotEmpty()) InfoRow("DNS", net.dns.joinToString(", "), copyable = true, showDivider = false)
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
            Text(formatSpeed(speed), style = MaterialTheme.typography.titleSmall, color = color)
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

private fun formatSpeed(bytesPerSec: Long): String {
    val kbps = bytesPerSec / 1024f
    val mbps = kbps / 1024f
    return when {
        mbps >= 1f -> "%.1f MB/s".format(mbps)
        kbps >= 1f -> "%.0f KB/s".format(kbps)
        else -> "${bytesPerSec} B/s"
    }
}
