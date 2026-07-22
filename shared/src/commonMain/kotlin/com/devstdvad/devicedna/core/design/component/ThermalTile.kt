package com.devstdvad.devicedna.core.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun ThermalTile(
    label: String,
    tempCelsius: Float?,
    modifier: Modifier = Modifier,
    temperatureText: String? = null,
    // When no numeric temperature is available (e.g. iOS exposes only a coarse thermal
    // state), pass the state name here so the tile shows it instead of a bare "—".
    stateName: String? = null,
) {
    val colors = AppTheme.colors
    val (statusColor, statusText, fraction) = when {
        tempCelsius == null && stateName != null -> when (stateName.lowercase()) {
            "critical" -> Triple(colors.critical, stringRes("component_thermal_state_label"), 1f)
            "serious" -> Triple(colors.thermalColor, stringRes("component_thermal_state_label"), 0.7f)
            "fair" -> Triple(colors.warning, stringRes("component_thermal_state_label"), 0.45f)
            else -> Triple(colors.sensorsColor, stringRes("component_thermal_state_label"), 0.2f) // Nominal
        }
        tempCelsius == null -> Triple(colors.textMuted, stringRes("common_na"), 0f)
        tempCelsius >= 70f -> Triple(colors.critical, stringRes("component_status_critical"), minOf(tempCelsius / 100f, 1f))
        tempCelsius >= 55f -> Triple(colors.critical.copy(alpha = 0.7f), stringRes("component_thermal_very_hot"), tempCelsius / 100f)
        tempCelsius >= 42f -> Triple(colors.thermalColor, stringRes("component_thermal_hot"), tempCelsius / 100f)
        tempCelsius >= 35f -> Triple(colors.warning, stringRes("component_thermal_warm"), tempCelsius / 100f)
        else -> Triple(colors.sensorsColor, stringRes("component_status_normal"), tempCelsius / 100f)
    }
    val animFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(700), label = "thermal")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, statusColor.copy(alpha = if (fraction > 0.5f) 0.35f else 0.15f), RoundedCornerShape(10.dp))
            .background(colors.surfaceElevated)
            .padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textMuted, maxLines = 1)
        Spacer(Modifier.height(5.dp))
        Text(
            text = when {
                tempCelsius != null -> temperatureText ?: "${Formatters.oneDecimal(tempCelsius)}°"
                stateName != null -> stateName
                else -> "—"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(colors.border),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animFraction)
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(colors.sensorsColor, statusColor),
                        ),
                    ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
