package com.devstdvad.devicedna.core.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme

@Composable
fun BatteryWidget(
    levelPercent: Int,
    isCharging: Boolean = false,
    accentColor: Color,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp,
    height: Dp = 120.dp,
) {
    val colors = AppTheme.colors
    val animatedLevel by animateFloatAsState(
        targetValue = levelPercent / 100f,
        animationSpec = tween(1000),
        label = "battery_fill",
    )
    val fillColor = when {
        levelPercent <= 15 -> colors.critical
        levelPercent <= 30 -> colors.warning
        else -> accentColor
    }

    Canvas(modifier = modifier.size(width, height)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 3.dp.toPx()
        val cornerRadius = 8.dp.toPx()
        val capW = w * 0.35f
        val capH = 6.dp.toPx()
        val bodyTop = capH
        val bodyH = h - capH
        val padding = strokeW + 3.dp.toPx()

        // Terminal cap
        drawRoundRect(
            color = colors.textMuted.copy(alpha = 0.5f),
            topLeft = Offset((w - capW) / 2, 0f),
            size = Size(capW, capH),
            cornerRadius = CornerRadius(3.dp.toPx()),
        )

        // Body outline
        val bodyPath = Path().apply {
            addRoundRect(RoundRect(
                left = strokeW / 2,
                top = bodyTop + strokeW / 2,
                right = w - strokeW / 2,
                bottom = h - strokeW / 2,
                cornerRadius = CornerRadius(cornerRadius),
            ))
        }
        drawPath(bodyPath, color = colors.border, style = Stroke(width = strokeW))

        // Fill
        val fillHeight = (bodyH - padding * 2) * animatedLevel
        if (fillHeight > 0f) {
            val fillTop = bodyTop + padding + (bodyH - padding * 2) * (1f - animatedLevel)
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(padding, fillTop),
                size = Size(w - padding * 2, fillHeight),
                cornerRadius = CornerRadius(cornerRadius - padding / 2),
            )
        }

        // Charging bolt
        if (isCharging) {
            val boltPath = Path().apply {
                val cx = w / 2; val cy = bodyTop + bodyH / 2
                val bw = 10.dp.toPx(); val bh = 18.dp.toPx()
                moveTo(cx + bw * 0.1f, cy - bh / 2)
                lineTo(cx - bw * 0.4f, cy + bh * 0.05f)
                lineTo(cx + bw * 0.1f, cy + bh * 0.05f)
                lineTo(cx - bw * 0.1f, cy + bh / 2)
                lineTo(cx + bw * 0.4f, cy - bh * 0.05f)
                lineTo(cx - bw * 0.1f, cy - bh * 0.05f)
                close()
            }
            drawPath(boltPath, color = Color.White.copy(alpha = 0.9f))
        }
    }
}
