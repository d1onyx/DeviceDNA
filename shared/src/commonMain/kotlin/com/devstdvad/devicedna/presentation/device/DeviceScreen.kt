package com.devstdvad.devicedna.presentation.device

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
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.PrivacyMask
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.AccentCard
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.core.design.component.StatusPill
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.di.resolveViewModel

@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = resolveViewModel(DeviceViewModel::class),
    settings: UserSettings = UserSettings(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: "Could not load device info"); return }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Identity hero card
        item {
            AccentCard(accentColor = colors.deviceColor) {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${info.manufacturer} · ${info.brand}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.deviceColor,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DeviceSpec("Model", info.model)
                    DeviceSpec("Codename", info.codename)
                    DeviceSpec("SoC", info.socName.ifBlank { "Unknown" }.take(12))
                }
                if (info.isRooted) {
                    Spacer(Modifier.height(10.dp))
                    StatusPill(MetricStatus.Warning, "Root detected")
                }
            }
        }

        item {
            SectionCard {
                Text("Identity", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Full Name", info.name)
                InfoRow("Model", info.model)
                InfoRow("Manufacturer", info.manufacturer)
                InfoRow("Brand", info.brand)
                InfoRow("Codename", info.codename, showDivider = false)
            }
        }

        item {
            AccentCard(accentColor = colors.deviceColor) {
                Text("Hardware", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Board", info.board, copyable = false)
                InfoRow("Hardware", info.hardware, copyable = false)
                InfoRow("SoC Model", info.socName.ifBlank { "Unknown" }, copyable = false)
                InfoRow("Bootloader", info.bootloader, copyable = false)
                InfoRow("Build Tags", info.buildTags.ifBlank { "Unknown" }, copyable = false)
                InfoRow("First API Level", info.firstApiLevel?.toString() ?: "Unknown", copyable = false)
                InfoRow("ABIs", info.supportedAbis.joinToString(", "), copyable = false, showDivider = false)
            }
        }

        item {
            SectionCard {
                Text("Identifiers", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(
                    label = "Android ID",
                    value = info.androidId,
                    maskedValue = if (settings.maskSensitive) PrivacyMask.maskDeviceId(info.androidId) else null,
                )
                InfoRow(
                    label = "Build Fingerprint",
                    value = info.buildFingerprint,
                    maskedValue = if (settings.maskSensitive) PrivacyMask.maskFingerprint(info.buildFingerprint) else null,
                )
                val serialVisible = settings.showImei && info.serialNumber.isNotBlank()
                InfoRow(
                    label = "Serial Number",
                    value = when {
                        !settings.showImei -> "Hidden in privacy settings"
                        info.serialNumber.isBlank() -> "Permission required"
                        else -> info.serialNumber
                    },
                    maskedValue = if (serialVisible && settings.maskSensitive) PrivacyMask.maskDeviceId(info.serialNumber) else null,
                    copyable = serialVisible,
                    showDivider = false,
                )
            }
        }

        item {
            AccentCard(accentColor = if (info.isRooted) colors.warning else colors.success) {
                Text("Security", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(
                    label = "Root Detected",
                    value = if (info.isRooted) "Yes — security risk" else "No",
                    copyable = false,
                )
                InfoRow(
                    label = "Emulator",
                    value = if (info.isEmulator) "Yes" else "No",
                    copyable = false,
                )
                InfoRow(
                    label = "Developer Options",
                    value = if (info.isDeveloperOptionsEnabled) "Enabled" else "Disabled",
                    copyable = false,
                )
                InfoRow(
                    label = "ADB",
                    value = if (info.isAdbEnabled) "Enabled" else "Disabled",
                    copyable = false,
                )
                InfoRow(
                    label = "Test Keys",
                    value = if (info.isTestKeysBuild) "Detected" else "No",
                    copyable = false,
                )
                InfoRow(
                    label = "Debuggable OS",
                    value = if (info.isDebuggableBuild) "Yes" else "No",
                    copyable = false,
                )
                InfoRow(
                    label = "Verified Boot",
                    value = info.verifiedBootState.ifBlank { "Unknown" },
                    copyable = false,
                )
                InfoRow(
                    label = "VBMeta State",
                    value = info.vbMetaDeviceState.ifBlank { "Unknown" },
                    copyable = false,
                )
                InfoRow(
                    label = "Flash Locked",
                    value = info.flashLocked.ifBlank { "Unknown" },
                    copyable = false,
                )
                InfoRow(
                    label = "Verity Mode",
                    value = info.verityMode.ifBlank { "Unknown" },
                    copyable = false,
                )
                InfoRow(
                    label = "Tamper Bit",
                    value = info.warrantyBit.ifBlank { "Unknown" },
                    copyable = false,
                )
                InfoRow(
                    label = "Suspicious Paths",
                    value = info.suspiciousRootPaths.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "None",
                    copyable = false,
                    showDivider = false,
                )
            }
        }

        item { Spacer(Modifier.height(contentPadding.calculateBottomPadding())) }
    }
}

@Composable
private fun DeviceSpec(label: String, value: String) {
    val colors = AppTheme.colors
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
    }
}
