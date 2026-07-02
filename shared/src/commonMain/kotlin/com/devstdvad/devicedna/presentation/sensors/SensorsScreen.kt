package com.devstdvad.devicedna.presentation.sensors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.domain.model.SensorDetails
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SensorsScreen(
    viewModel: SensorsViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${state.info?.sensors?.size ?: 0}",
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.sensorsColor,
                    )
                    Text("Sensors detected", style = MaterialTheme.typography.bodyMedium, color = colors.textMuted)
                }
                val wakeUpCount = state.info?.sensors?.count { it.isWakeUp } ?: 0
                if (wakeUpCount > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("$wakeUpCount", style = MaterialTheme.typography.displaySmall, color = colors.accent)
                        Text("Wake-up sensors", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQuery,
                placeholder = { Text("Search sensors…", color = colors.textMuted) },
                leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.textMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.sensorsColor,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.sensorsColor,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                ),
                singleLine = true,
            )
        }

        items(state.filtered) { sensor ->
            SensorItem(sensor)
        }
    }
}

@Composable
private fun SensorItem(sensor: SensorDetails) {
    val colors = AppTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (sensor.isWakeUp) colors.sensorsColor.copy(alpha = 0.2f) else colors.border, RoundedCornerShape(10.dp)),
        color = colors.surfaceElevated,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sensor.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text("${sensor.typeName} · ${sensor.vendor}", style = MaterialTheme.typography.bodySmall, color = colors.textMuted)
                Text(
                    "Power: ${sensor.powerMa} mA · Max: ${sensor.maxRange} · Res: ${sensor.resolution}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (sensor.isWakeUp) {
                    Text(
                        "Wake-up",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.sensorsColor,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.sensorsColor.copy(alpha = 0.1f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
                if (sensor.isDynamic) {
                    Text(
                        "Dynamic",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.accent.copy(alpha = 0.1f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}
