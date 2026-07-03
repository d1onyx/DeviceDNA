package com.devstdvad.devicedna.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun StatusPill(
    status: MetricStatus,
    label: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val (bg, fg, text) = when (status) {
        MetricStatus.Normal -> Triple(colors.success.copy(alpha = 0.15f), colors.success, label ?: stringRes("component_status_normal"))
        MetricStatus.Warning -> Triple(colors.warning.copy(alpha = 0.15f), colors.warning, label ?: stringRes("component_status_warning"))
        MetricStatus.Critical -> Triple(colors.critical.copy(alpha = 0.15f), colors.critical, label ?: stringRes("component_status_critical"))
        MetricStatus.PermissionRequired -> Triple(colors.accent.copy(alpha = 0.15f), colors.accent, label ?: stringRes("component_status_permission"))
        MetricStatus.Unavailable -> Triple(colors.border, colors.textMuted, label ?: stringRes("common_unavailable"))
        MetricStatus.Unknown -> Triple(colors.border, colors.textMuted, label ?: stringRes("common_unknown"))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun statusColor(status: MetricStatus): Color {
    val colors = AppTheme.colors
    return when (status) {
        MetricStatus.Normal -> colors.success
        MetricStatus.Warning -> colors.warning
        MetricStatus.Critical -> colors.critical
        MetricStatus.PermissionRequired -> colors.accent
        MetricStatus.Unavailable, MetricStatus.Unknown -> colors.textMuted
    }
}
