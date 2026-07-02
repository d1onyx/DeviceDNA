package com.devstdvad.devicedna.presentation.tests

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.di.resolveViewModel

@Composable
fun TestsScreen(
    viewModel: TestsViewModel = resolveViewModel(TestsViewModel::class),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors
    val passedCount = state.tests.count { it.status == HardwareTestStatus.Passed }
    val warningCount = state.tests.count { it.status == HardwareTestStatus.Warning }
    val failedCount = state.tests.count { it.status == HardwareTestStatus.Failed }
    val unavailableCount = state.tests.count { it.status == HardwareTestStatus.Unavailable }
    val totalCount = state.tests.size.coerceAtLeast(1)
    val progress by animateFloatAsState(
        targetValue = passedCount / totalCount.toFloat(),
        animationSpec = tween(500),
        label = "hardware_test_progress",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp + contentPadding.calculateTopPadding(),
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard {
                Text("Hardware Diagnostics", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(state.summary, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = if (failedCount > 0) colors.critical else colors.success,
                    trackColor = colors.border,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TestCounter("Passed", passedCount.toString(), colors.success, Modifier.weight(1f))
                    TestCounter("Warnings", warningCount.toString(), colors.warning, Modifier.weight(1f))
                    TestCounter("Failed", failedCount.toString(), colors.critical, Modifier.weight(1f))
                    TestCounter("N/A", unavailableCount.toString(), colors.textMuted, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = viewModel::runTests,
                    enabled = !state.isRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, tint = colors.background, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(if (state.isRunning) "Running checks" else "Run hardware checks", color = colors.background)
                }
            }
        }

        val groups = state.tests.groupBy { it.group }
        groups.forEach { (group, tests) ->
            item {
                Text(group, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            }
            tests.forEach { test ->
                item { HardwareTestRow(test) }
            }
        }
    }
}

@Composable
private fun TestCounter(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Surface(
        modifier = modifier.border(1.dp, colors.border, RoundedCornerShape(8.dp)),
        color = colors.surface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
        }
    }
}

@Composable
private fun HardwareTestRow(test: HardwareTestResult) {
    val colors = AppTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
        color = colors.surfaceElevated,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceHover),
                contentAlignment = Alignment.Center,
            ) {
                Icon(test.icon, contentDescription = null, tint = statusColor(test.status), modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(test.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(test.detail, style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
            }
            TestStatusIcon(test.status)
        }
    }
}

@Composable
private fun TestStatusIcon(status: HardwareTestStatus) {
    val colors = AppTheme.colors
    val (bg, fg, icon) = when (status) {
        HardwareTestStatus.Passed -> Triple(colors.success.copy(alpha = 0.18f), colors.success, Icons.Outlined.Check)
        HardwareTestStatus.Warning -> Triple(colors.warning.copy(alpha = 0.18f), colors.warning, Icons.AutoMirrored.Outlined.Help)
        HardwareTestStatus.Failed -> Triple(colors.critical.copy(alpha = 0.18f), colors.critical, Icons.Outlined.Close)
        HardwareTestStatus.Unavailable -> Triple(colors.border, colors.textMuted, Icons.Outlined.Close)
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun statusColor(status: HardwareTestStatus): Color {
    val colors = AppTheme.colors
    return when (status) {
        HardwareTestStatus.Passed -> colors.success
        HardwareTestStatus.Warning -> colors.warning
        HardwareTestStatus.Failed -> colors.critical
        HardwareTestStatus.Unavailable -> colors.textMuted
    }
}

internal fun defaultIconForGroup(group: String): ImageVector = when (group) {
    "Device" -> Icons.Outlined.Security
    "Performance" -> Icons.Outlined.Speed
    "Power" -> Icons.Outlined.BatteryChargingFull
    "Memory" -> Icons.Outlined.Memory
    "Storage" -> Icons.Outlined.Storage
    "Display" -> Icons.Outlined.DisplaySettings
    "Camera" -> Icons.Outlined.CameraAlt
    "Thermal" -> Icons.Outlined.Thermostat
    "Sensors" -> Icons.Outlined.Sensors
    "Network" -> Icons.Outlined.NetworkCheck
    "Connectivity" -> Icons.Outlined.Bluetooth
    "Apps" -> Icons.Outlined.Apps
    else -> Icons.Outlined.Power
}
