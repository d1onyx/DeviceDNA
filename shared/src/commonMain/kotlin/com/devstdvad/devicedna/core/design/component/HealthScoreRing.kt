package com.devstdvad.devicedna.core.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme

@Composable
fun HealthScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
) {
    val colors = AppTheme.colors
    val animatedSweep by animateFloatAsState(
        targetValue = (score / 100f) * 360f,
        animationSpec = tween(900),
        label = "ring",
    )
    val ringColor = when {
        score >= 80 -> colors.success
        score >= 60 -> colors.warning
        score >= 40 -> colors.warning
        else -> colors.critical
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)

            drawArc(
                color = colors.border,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            if (animatedSweep > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineSmall,
                color = ringColor,
            )
            Text(text = "/100", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
        }
    }
}
