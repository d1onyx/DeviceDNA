package com.devstdvad.devicedna.presentation.system

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.PhoneIphone
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
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.AccentCard
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.di.resolveViewModel
import com.devstdvad.devicedna.platform.PlatformInfo
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun SystemScreen(
    viewModel: SystemViewModel = resolveViewModel(SystemViewModel::class),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors
    val isIos = PlatformInfo.isIos

    if (state.isLoading) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: stringRes("system_load_error")); return }

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
        // Hero: Android version
        item {
            AccentCard(accentColor = colors.deviceColor) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    androidx.compose.foundation.layout.Column {
                        Text(
                            text = if (isIos) info.androidVersion else stringRes("system_android_version_prefix", info.androidVersion),
                            style = MaterialTheme.typography.displaySmall,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = if (isIos) stringRes("system_build_prefix", info.buildNumber) else stringRes("system_api_release", info.apiLevel, info.releaseName),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.deviceColor,
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (info.runningProcessCount > 0) {
                                InfoChip(stringRes("system_processes_count", info.runningProcessCount))
                            }
                            if (info.totalRamGb > 0f) {
                                InfoChip(stringRes("system_ram_chip", Formatters.oneDecimal(info.totalRamGb)))
                            }
                            InfoChip(formatUptime(info.uptimeMillis))
                        }
                    }
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                            .background(colors.deviceColor.copy(alpha = 0.12f))
                            .border(1.dp, colors.deviceColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (isIos) Icons.Outlined.PhoneIphone else Icons.Outlined.Android, null, tint = colors.deviceColor, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        item {
            SectionCard {
                Text(if (isIos) stringRes("system_section_os") else stringRes("system_section_android"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(stringRes("system_field_version"), info.androidVersion)
                if (!isIos) {
                    InfoRow(stringRes("system_field_api_level"), info.apiLevel.toString())
                    InfoRow(stringRes("device_field_codename"), info.releaseName)
                    InfoRow(stringRes("system_field_security_patch"), info.securityPatchLevel)
                }
                InfoRow(stringRes("system_field_build_number"), info.buildNumber)
                InfoRow(stringRes("system_field_build_type"), info.buildType, showDivider = false)
            }
        }

        item {
            AccentCard(accentColor = colors.cpuColor) {
                Text(stringRes("system_section_runtime"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(stringRes("system_field_kernel"), info.kernelVersion)
                if (isIos) {
                    InfoRow(stringRes("system_field_graphics"), info.openGlVersion)
                    InfoRow(stringRes("system_field_low_power_mode"), if (info.isPowerSaveMode) stringRes("common_on") else stringRes("common_off"))
                    InfoRow(stringRes(if (isIos) "system_field_app_uptime" else "system_field_uptime"), formatUptime(info.uptimeMillis), showDivider = false)
                } else {
                    InfoRow(stringRes("system_field_java_vm"), info.javaVm)
                    InfoRow(stringRes("system_field_opengl_es"), info.openGlVersion)
                    if (info.glEsVersion.isNotBlank() && info.glEsVersion != "3.2") {
                        InfoRow(stringRes("system_field_gl_es_version"), info.glEsVersion)
                    }
                    InfoRow(stringRes("system_field_baseband"), info.baseband)
                    InfoRow(stringRes("device_field_bootloader"), info.bootloader)
                    InfoRow(stringRes("system_field_uptime"), formatUptime(info.uptimeMillis), showDivider = false)
                }
            }
        }

        item {
            SectionCard {
                Text(stringRes("device_section_security"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                if (!isIos) {
                    InfoRow(stringRes("system_field_selinux"), info.seLinuxStatus.ifBlank { stringRes("system_value_enforcing") }, copyable = false)
                }
                InfoRow(
                    label = if (isIos) stringRes("system_field_data_protection") else stringRes("system_field_encrypted"),
                    value = if (info.isEncrypted) (if (isIos) stringRes("common_enabled") else stringRes("common_yes")) else stringRes("common_no"),
                    copyable = false,
                    showDivider = false,
                )
            }
        }

        item {
            SectionCard {
                Text(stringRes("system_section_app_integrity"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(stringRes("system_field_package"), info.packageName)
                InfoRow(stringRes("system_field_version"), "${info.appVersionName} (${info.appVersionCode})", copyable = false)
                InfoRow(stringRes("system_field_installer"), info.installerPackageName ?: stringRes("common_unknown"))
                InfoRow(
                    stringRes("system_field_known_store"),
                    if (!info.supportsInstallSourceInspection) stringRes("common_unknown") else if (info.isInstalledFromKnownStore) stringRes("common_yes") else stringRes("common_no"),
                    copyable = false,
                    showDivider = isIos,
                )
                if (!isIos) {
                    InfoRow(stringRes("system_field_debuggable_app"), if (info.isAppDebuggable) stringRes("common_yes") else stringRes("common_no"), copyable = false)
                    InfoRow(
                        label = stringRes("system_field_signing_sha256"),
                        value = info.signingCertificateSha256 ?: stringRes("common_unavailable"),
                        copyable = info.signingCertificateSha256 != null,
                        showDivider = false,
                    )
                }
            }
        }

        item {
            SectionCard {
                Text(stringRes("system_section_locale"), style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow(stringRes("system_field_language"), info.language)
                InfoRow(stringRes("system_field_time_zone"), info.timeZone, showDivider = false)
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    val colors = AppTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.textSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.surfaceHover)
            .border(1.dp, colors.border, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun formatUptime(millis: Long): String {
    val totalMinutes = millis.coerceAtLeast(0L) / 60_000L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes / 60L) % 24L
    return if (days > 0) stringRes("system_uptime_days_hours", days, hours) else stringRes("system_uptime_hours", hours)
}
