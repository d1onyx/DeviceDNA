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
import com.devstdvad.devicedna.platform.PlatformInfo
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun DeviceScreen(
    viewModel: DeviceViewModel = resolveViewModel(DeviceViewModel::class),
    settings: UserSettings = UserSettings(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors
    val isIos = PlatformInfo.isIos

    if (state.isLoading) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: stringRes("device_load_error")); return }

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
                    DeviceSpec(stringRes("device_field_model"), info.model)
                    if (isIos) {
                        DeviceSpec(stringRes("device_field_identifier"), info.hardware.ifBlank { stringRes("common_unknown") })
                    } else {
                        DeviceSpec(stringRes("device_field_codename"), info.codename)
                    }
                    DeviceSpec(stringRes("device_field_soc"), info.socName.ifBlank { stringRes("common_unknown") }.take(12))
                }
                if (info.isRooted) {
                    Spacer(Modifier.height(10.dp))
                    StatusPill(MetricStatus.Warning, if (isIos) stringRes("device_jailbreak_detected_pill") else stringRes("device_root_detected_pill"))
                }
            }
        }

        item {
            SectionCard {
                Text(stringRes("device_section_identity"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(stringRes("device_field_full_name"), info.name)
                InfoRow(stringRes("device_field_model"), info.model)
                InfoRow(stringRes("device_field_manufacturer"), info.manufacturer)
                InfoRow(stringRes("device_field_brand"), info.brand, showDivider = isIos)
                if (isIos) {
                    InfoRow(stringRes("device_field_identifier"), info.hardware.ifBlank { stringRes("common_unknown") }, showDivider = false)
                } else {
                    InfoRow(stringRes("device_field_codename"), info.codename, showDivider = false)
                }
            }
        }

        item {
            AccentCard(accentColor = colors.deviceColor) {
                Text(stringRes("device_section_hardware"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                if (isIos) {
                    InfoRow(stringRes("device_field_chip"), info.socName.ifBlank { stringRes("common_unknown") }, copyable = false)
                    InfoRow(stringRes("device_field_identifier"), info.hardware, copyable = false)
                    InfoRow(stringRes("device_field_architecture"), info.supportedAbis.joinToString(", "), copyable = false, showDivider = false)
                } else {
                    InfoRow(stringRes("device_field_board"), info.board, copyable = false)
                    InfoRow(stringRes("device_section_hardware"), info.hardware, copyable = false)
                    InfoRow(stringRes("device_field_soc_model"), info.socName.ifBlank { stringRes("common_unknown") }, copyable = false)
                    InfoRow(stringRes("device_field_bootloader"), info.bootloader, copyable = false)
                    InfoRow(stringRes("device_field_build_tags"), info.buildTags.ifBlank { stringRes("common_unknown") }, copyable = false)
                    InfoRow(stringRes("device_field_first_api_level"), info.firstApiLevel?.toString() ?: stringRes("common_unknown"), copyable = false)
                    InfoRow(stringRes("device_field_abis"), info.supportedAbis.joinToString(", "), copyable = false, showDivider = false)
                }
            }
        }

        item {
            SectionCard {
                Text(stringRes("device_section_identifiers"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(
                    label = if (isIos) stringRes("device_field_identifier_for_vendor") else stringRes("device_field_android_id"),
                    value = info.androidId,
                    maskedValue = if (settings.maskSensitive) PrivacyMask.maskDeviceId(info.androidId) else null,
                    showDivider = !isIos,
                )
                if (!isIos) {
                    InfoRow(
                        label = stringRes("device_field_build_fingerprint"),
                        value = info.buildFingerprint,
                        maskedValue = if (settings.maskSensitive) PrivacyMask.maskFingerprint(info.buildFingerprint) else null,
                    )
                    val serialVisible = settings.showImei && info.serialNumber.isNotBlank()
                    InfoRow(
                        label = stringRes("device_field_serial_number"),
                        value = when {
                            !settings.showImei -> stringRes("common_hidden_privacy_settings")
                            info.serialNumber.isBlank() -> stringRes("common_permission_required")
                            else -> info.serialNumber
                        },
                        maskedValue = if (serialVisible && settings.maskSensitive) PrivacyMask.maskDeviceId(info.serialNumber) else null,
                        copyable = serialVisible,
                        showDivider = false,
                    )
                }
            }
        }

        item {
            AccentCard(accentColor = if (info.isRooted) colors.warning else colors.success) {
                Text(stringRes("device_section_security"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(
                    label = if (isIos) stringRes("device_field_jailbreak_detected") else stringRes("device_field_root_detected"),
                    value = if (info.isRooted) stringRes("device_value_security_risk") else stringRes("common_no"),
                    copyable = false,
                )
                InfoRow(
                    label = if (isIos) stringRes("device_field_simulator") else stringRes("device_field_emulator"),
                    value = if (info.isEmulator) stringRes("common_yes") else stringRes("common_no"),
                    copyable = false,
                    showDivider = !isIos || info.suspiciousRootPaths.isNotEmpty(),
                )
                if (!isIos) {
                    InfoRow(
                        label = stringRes("device_field_developer_options"),
                        value = if (info.isDeveloperOptionsEnabled) stringRes("common_enabled") else stringRes("common_disabled"),
                        copyable = false,
                    )
                    InfoRow(
                        label = stringRes("device_field_adb"),
                        value = if (info.isAdbEnabled) stringRes("common_enabled") else stringRes("common_disabled"),
                        copyable = false,
                    )
                    InfoRow(
                        label = stringRes("device_field_test_keys"),
                        value = if (info.isTestKeysBuild) stringRes("common_detected") else stringRes("common_no"),
                        copyable = false,
                    )
                    InfoRow(
                        label = stringRes("device_field_debuggable_os"),
                        value = if (info.isDebuggableBuild) stringRes("common_yes") else stringRes("common_no"),
                        copyable = false,
                    )
                    InfoRow(
                        label = stringRes("device_field_verified_boot"),
                        value = info.verifiedBootState.ifBlank { stringRes("common_unknown") },
                        copyable = false,
                    )
                    InfoRow(
                        label = stringRes("device_field_vbmeta_state"),
                        value = info.vbMetaDeviceState.ifBlank { stringRes("common_unknown") },
                        copyable = false,
                    )
                    InfoRow(
                        label = stringRes("device_field_flash_locked"),
                        value = info.flashLocked.ifBlank { stringRes("common_unknown") },
                        copyable = false,
                    )
                    InfoRow(
                        label = stringRes("device_field_verity_mode"),
                        value = info.verityMode.ifBlank { stringRes("common_unknown") },
                        copyable = false,
                    )
                    InfoRow(
                        label = stringRes("device_field_tamper_bit"),
                        value = info.warrantyBit.ifBlank { stringRes("common_unknown") },
                        copyable = false,
                    )
                }
                if (!isIos || info.suspiciousRootPaths.isNotEmpty()) {
                    InfoRow(
                        label = stringRes("device_field_suspicious_paths"),
                        value = info.suspiciousRootPaths.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: stringRes("common_none"),
                        copyable = false,
                        showDivider = false,
                    )
                }
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
