package com.devstdvad.devicedna.presentation.system

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
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.SettingsInputAntenna
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
import com.devstdvad.devicedna.presentation.connectivity.ConnectivityScreen
import com.devstdvad.devicedna.presentation.network.NetworkScreen
import com.devstdvad.devicedna.presentation.sensors.SensorsScreen
import kotlinx.coroutines.launch

private data class SystemTab(
    val label: String,
    val icon: ImageVector,
    val accentColor: @Composable () -> Color,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SystemHubScreen(contentPadding: PaddingValues = PaddingValues()) {
    val colors = AppTheme.colors

    val tabs = listOf(
        SystemTab("OS", Icons.Outlined.PhoneAndroid) { colors.deviceColor },
        SystemTab("Network", Icons.Outlined.NetworkCheck) { colors.networkColor },
        SystemTab("Radio", Icons.Outlined.SettingsInputAntenna) { colors.accent },
        SystemTab("Sensors", Icons.Outlined.Sensors) { colors.sensorsColor },
    )

    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = pagerState.currentPage == index
                val accent = tab.accentColor()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) accent.copy(alpha = 0.12f) else colors.surfaceElevated)
                        .border(1.dp, if (selected) accent.copy(alpha = 0.4f) else colors.border, RoundedCornerShape(10.dp))
                        .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tab.icon, null, tint = if (selected) accent else colors.textMuted, modifier = Modifier.size(16.dp))
                        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = if (selected) accent else colors.textMuted)
                    }
                }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f), beyondViewportPageCount = 1) { page ->
            when (page) {
                0 -> SystemScreen()
                1 -> NetworkScreen()
                2 -> ConnectivityScreen()
                3 -> SensorsScreen()
            }
        }
    }
}
