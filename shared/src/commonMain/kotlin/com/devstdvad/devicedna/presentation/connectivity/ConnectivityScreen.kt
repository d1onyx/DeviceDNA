package com.devstdvad.devicedna.presentation.connectivity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.presentation.network.NetworkViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConnectivityScreen(
    viewModel: NetworkViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) {
        LoadingScreen("Reading radio capabilities...")
        return
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
        state.connectivity?.let { conn ->
            item {
                SectionCard {
                    Text("Wi-Fi", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow("Wi-Fi hardware", supported(conn.hasWifi))
                    InfoRow("Wi-Fi Direct", supported(conn.hasWifiDirect))
                    InfoRow("5 GHz band", supported(conn.hasWifi5Ghz))
                    InfoRow("6 GHz band", supported(conn.hasWifi6Ghz))
                    InfoRow(
                        label = "Standards",
                        value = conn.wifiStandards.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "Restricted by platform",
                        showDivider = false,
                    )
                }
            }
            item {
                SectionCard {
                    Text("Personal Area", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow("Bluetooth", supported(conn.hasBluetooth))
                    InfoRow("Bluetooth LE", supported(conn.hasBluetoothLe))
                    InfoRow("Bluetooth version", conn.bluetoothVersion ?: "Restricted by platform")
                    InfoRow("NFC", supported(conn.hasNfc))
                    InfoRow("UWB", supported(conn.hasUwb), showDivider = false)
                }
            }
            item {
                SectionCard {
                    Text("Mobile", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow("eSIM", supported(conn.hasEsim))
                    InfoRow("Cellular detail", "Available only with carrier/runtime permission", showDivider = false)
                }
            }
        }
    }
}

private fun supported(value: Boolean): String = if (value) "Supported" else "Not supported"
