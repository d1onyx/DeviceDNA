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
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.AccentCard
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SystemScreen(
    viewModel: SystemViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: "Could not load system info"); return }

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
                            text = "Android ${info.androidVersion}",
                            style = MaterialTheme.typography.displaySmall,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "API ${info.apiLevel} · ${info.releaseName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.deviceColor,
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (info.runningProcessCount > 0) {
                                InfoChip("${info.runningProcessCount} Processes")
                            }
                            if (info.totalRamGb > 0f) {
                                InfoChip("%.1f GB RAM".format(info.totalRamGb))
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
                        Icon(Icons.Outlined.Android, null, tint = colors.deviceColor, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        item {
            SectionCard {
                Text("Android", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Version", info.androidVersion)
                InfoRow("API Level", info.apiLevel.toString())
                InfoRow("Codename", info.releaseName)
                InfoRow("Security Patch", info.securityPatchLevel)
                InfoRow("Build Number", info.buildNumber)
                InfoRow("Build Type", info.buildType, showDivider = false)
            }
        }

        item {
            AccentCard(accentColor = colors.cpuColor) {
                Text("Runtime", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Kernel", info.kernelVersion)
                InfoRow("Java VM", info.javaVm)
                InfoRow("OpenGL ES", info.openGlVersion)
                if (info.glEsVersion.isNotBlank() && info.glEsVersion != "3.2") {
                    InfoRow("GL ES Version", info.glEsVersion)
                }
                InfoRow("Baseband", info.baseband)
                InfoRow("Bootloader", info.bootloader)
                InfoRow("Uptime", formatUptime(info.uptimeMillis), showDivider = false)
            }
        }

        item {
            SectionCard {
                Text("Security", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("SELinux", info.seLinuxStatus.ifBlank { "Enforcing" }, copyable = false)
                InfoRow("Encrypted", if (info.isEncrypted) "Yes" else "No", copyable = false, showDivider = false)
            }
        }

        item {
            SectionCard {
                Text("App Integrity", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Package", info.packageName)
                InfoRow("Version", "${info.appVersionName} (${info.appVersionCode})", copyable = false)
                InfoRow("Installer", info.installerPackageName ?: "Unknown")
                InfoRow("Known Store", if (info.isInstalledFromKnownStore) "Yes" else "No", copyable = false)
                InfoRow("Debuggable App", if (info.isAppDebuggable) "Yes" else "No", copyable = false)
                InfoRow(
                    label = "Signing SHA-256",
                    value = info.signingCertificateSha256 ?: "Unavailable",
                    copyable = info.signingCertificateSha256 != null,
                    showDivider = false,
                )
            }
        }

        item {
            SectionCard {
                Text("Locale", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                InfoRow("Language", info.language)
                InfoRow("Time Zone", info.timeZone, showDivider = false)
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

private fun formatUptime(millis: Long): String {
    val totalMinutes = millis.coerceAtLeast(0L) / 60_000L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes / 60L) % 24L
    return if (days > 0) "${days}d ${hours}h up" else "${hours}h up"
}
