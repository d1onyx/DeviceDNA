package com.devstdvad.devicedna.core.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme

@Composable
fun MetricCard(
    title: String,
    primaryValue: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    secondaryValue: String? = null,
    progress: Float? = null,
    status: MetricStatus = MetricStatus.Normal,
    accentColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val resolvedColor = accentColor ?: statusColor(status)
    val animatedProgress by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = tween(600),
        label = "progress",
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, resolvedColor.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = colors.surfaceElevated,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary,
                )
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                        .background(resolvedColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedColor,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            if (secondaryValue != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = secondaryValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                )
            }
            if (progress != null) {
                Spacer(Modifier.height(10.dp))
                LiveBar(fraction = animatedProgress, accentColor = resolvedColor)
            }
        }
    }
}
