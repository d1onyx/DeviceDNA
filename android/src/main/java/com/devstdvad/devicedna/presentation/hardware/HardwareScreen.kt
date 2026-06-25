package com.devstdvad.devicedna.presentation.hardware

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.presentation.battery.BatteryScreen
import com.devstdvad.devicedna.presentation.camera.CameraScreen
import com.devstdvad.devicedna.presentation.cpu.CpuScreen
import com.devstdvad.devicedna.presentation.device.DeviceScreen
import com.devstdvad.devicedna.presentation.display.DisplayScreen
import com.devstdvad.devicedna.presentation.thermal.ThermalScreen
import kotlinx.coroutines.launch

private data class HardwareTab(
    val label: String,
    val icon: ImageVector,
    val accentColor: @Composable () -> Color,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HardwareScreen(contentPadding: PaddingValues = PaddingValues()) {
    val colors = AppTheme.colors

    val tabs = listOf(
        HardwareTab("Device", Icons.Outlined.PhoneAndroid) { colors.deviceColor },
        HardwareTab("CPU", Icons.Outlined.Memory) { colors.cpuColor },
        HardwareTab("Battery", Icons.Outlined.BatteryFull) { colors.batteryColor },
        HardwareTab("Display", Icons.Outlined.DisplaySettings) { colors.displayColor },
        HardwareTab("Camera", Icons.Outlined.CameraAlt) { colors.cameraColor },
        HardwareTab("Thermal", Icons.Outlined.Thermostat) { colors.thermalColor },
    )

    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        // Section tab strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = pagerState.currentPage == index
                val accent = tab.accentColor()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) accent.copy(alpha = 0.12f) else colors.surfaceElevated,
                        )
                        .border(
                            1.dp,
                            if (selected) accent.copy(alpha = 0.4f) else colors.border,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (selected) accent else colors.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) accent else colors.textMuted,
                        )
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> DeviceScreen()
                1 -> CpuScreen()
                2 -> BatteryScreen()
                3 -> DisplayScreen()
                4 -> CameraScreen()
                5 -> ThermalScreen()
            }
        }
    }
}
