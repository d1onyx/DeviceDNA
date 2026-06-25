package com.devstdvad.devicedna.core.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme

@Composable
fun CpuCoreTile(
    coreIndex: Int,
    frequencyMhz: Int?,
    maxFrequencyMhz: Int,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
) {
    val colors = AppTheme.colors
    val fraction = if (frequencyMhz != null && maxFrequencyMhz > 0) {
        (frequencyMhz.toFloat() / maxFrequencyMhz).coerceIn(0f, 1f)
    } else 0f
    val animated by animateFloatAsState(targetValue = fraction, animationSpec = tween(500), label = "core$coreIndex")
    val barColor = when {
        !isOnline -> colors.textMuted
        fraction >= 0.85f -> colors.critical
        fraction >= 0.60f -> colors.warning
        else -> colors.cpuColor
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (fraction > 0.6f) barColor.copy(alpha = 0.3f) else colors.border, RoundedCornerShape(10.dp))
            .background(colors.surfaceElevated)
            .padding(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape)
                        .background(if (isOnline) barColor else colors.textMuted),
                )
                Text("C$coreIndex", style = MaterialTheme.typography.labelMedium, color = colors.textMuted)
            }
            Text(
                text = if (frequencyMhz != null && isOnline) "${frequencyMhz}M" else if (!isOnline) "off" else "—",
                style = MaterialTheme.typography.labelMedium,
                color = if (isOnline) colors.textPrimary else colors.textMuted,
            )
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.border),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(animated).height(4.dp).background(barColor),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${(fraction * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = barColor,
        )
    }
}
