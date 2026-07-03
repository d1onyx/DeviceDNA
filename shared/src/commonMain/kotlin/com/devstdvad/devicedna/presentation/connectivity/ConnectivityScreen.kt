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
import com.devstdvad.devicedna.di.resolveViewModel
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun ConnectivityScreen(
    viewModel: NetworkViewModel = resolveViewModel(NetworkViewModel::class),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) {
        LoadingScreen(stringRes("connectivity_loading"))
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
                    Text(stringRes("network_type_wifi"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow(stringRes("connectivity_field_wifi_hardware"), supported(conn.hasWifi))
                    InfoRow(stringRes("connectivity_field_wifi_direct"), supported(conn.hasWifiDirect))
                    InfoRow(stringRes("connectivity_field_5ghz"), supported(conn.hasWifi5Ghz))
                    InfoRow(stringRes("connectivity_field_6ghz"), supported(conn.hasWifi6Ghz))
                    InfoRow(
                        label = stringRes("connectivity_field_standards"),
                        value = conn.wifiStandards.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: stringRes("connectivity_value_restricted"),
                        showDivider = false,
                    )
                }
            }
            item {
                SectionCard {
                    Text(stringRes("connectivity_section_personal_area"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow(stringRes("connectivity_field_bluetooth"), supported(conn.hasBluetooth))
                    InfoRow(stringRes("connectivity_field_bluetooth_le"), supported(conn.hasBluetoothLe))
                    InfoRow(stringRes("connectivity_field_bluetooth_version"), conn.bluetoothVersion ?: stringRes("connectivity_value_restricted"))
                    InfoRow(stringRes("connectivity_field_nfc"), supported(conn.hasNfc))
                    InfoRow(stringRes("connectivity_field_uwb"), supported(conn.hasUwb), showDivider = false)
                }
            }
            item {
                SectionCard {
                    Text(stringRes("connectivity_section_mobile"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    InfoRow(stringRes("connectivity_field_esim"), supported(conn.hasEsim))
                    InfoRow(stringRes("connectivity_field_cellular_detail"), stringRes("connectivity_value_cellular_detail"), showDivider = false)
                }
            }
        }
    }
}

@Composable
private fun supported(value: Boolean): String = if (value) stringRes("connectivity_value_supported") else stringRes("connectivity_value_not_supported")
