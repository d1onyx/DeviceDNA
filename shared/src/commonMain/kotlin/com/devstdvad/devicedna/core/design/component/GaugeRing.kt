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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devstdvad.devicedna.core.design.AppTheme

@Composable
fun GaugeRing(
    value: Float,
    maxValue: Float = 100f,
    label: String,
    valueText: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 10.dp,
    subLabel: String? = null,
) {
    val colors = AppTheme.colors
    val fraction = (value / maxValue).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = fraction * 270f,
        animationSpec = tween(900),
        label = "gauge",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val startAngle = 135f

            // Track
            drawArc(
                color = colors.border,
                startAngle = startAngle,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            // Fill
            if (animatedSweep > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = startAngle,
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
                text = valueText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = when {
                        size >= 100.dp -> 20.sp
                        size >= 80.dp -> 16.sp
                        else -> 13.sp
                    },
                ),
                color = colors.textPrimary,
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )
        }
    }
}
