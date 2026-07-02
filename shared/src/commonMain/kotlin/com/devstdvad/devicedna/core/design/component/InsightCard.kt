package com.devstdvad.devicedna.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.domain.model.HealthInsight
import com.devstdvad.devicedna.domain.model.InsightSeverity

@Composable
fun InsightCard(
    insight: HealthInsight,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val (accentColor, icon) = when (insight.severity) {
        InsightSeverity.Critical -> colors.critical to Icons.Outlined.Warning
        InsightSeverity.Warning -> colors.warning to Icons.Outlined.Warning
        InsightSeverity.Info -> colors.info to Icons.Outlined.Info
        InsightSeverity.Good -> colors.success to Icons.Outlined.CheckCircle
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.07f))
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(insight.title, style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
            Text(insight.summary, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}
