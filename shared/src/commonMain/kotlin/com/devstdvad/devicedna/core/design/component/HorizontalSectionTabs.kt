package com.devstdvad.devicedna.core.design.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme

data class SectionTab(val id: String, val label: String, val icon: ImageVector)

@Composable
fun HorizontalSectionTabs(
    tabs: List<SectionTab>,
    selectedId: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val scrollState = rememberScrollState()

    val topPadding by animateDpAsState(targetValue = 8.dp, animationSpec = tween(250), label = "tabs_padding")

    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = topPadding),
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.id == selectedId
                val indicatorWidth by animateDpAsState(
                    targetValue = if (isSelected) 28.dp else 0.dp,
                    animationSpec = tween(220),
                    label = "tab_indicator",
                )
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) {
                                Modifier
                                    .background(colors.surfaceElevated)
                                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onTabSelected(tab.id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                        tint = if (isSelected) colors.accent else colors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) colors.textPrimary else colors.textMuted,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .size(width = indicatorWidth, height = 2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(colors.accent),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colors.border),
        )
    }
}
